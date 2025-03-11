/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.DnsZone;
import com.foilen.clouds.manager.services.model.SecretStore;
import com.foilen.clouds.manager.services.model.WebApp;
import com.foilen.smalltools.crypt.bouncycastle.asymmetric.AsymmetricKeys;
import com.foilen.smalltools.crypt.bouncycastle.asymmetric.RSACrypt;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSATools;
import com.foilen.smalltools.tools.*;
import com.google.common.base.Joiner;
import org.shredzone.acme4j.*;
import org.shredzone.acme4j.challenge.Challenge;
import org.shredzone.acme4j.challenge.Dns01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TXTRecord;
import org.xbill.DNS.Type;

import java.net.URI;
import java.net.URL;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.*;

@Component
public class LetsEncryptService extends AbstractBasics {

    protected static final String CA_CERTIFICATE_TEXT_PEM = ResourceTools.getResourceAsString("/com/foilen/clouds/manager/services/lets-encrypt-r3.pem");

    @Autowired
    private CloudService cloudService;

    private String getChallengeErrorDetails(Challenge challenge) {
        logger.info("getChallengeErrorDetails: {}", challenge);
        if (challenge != null && challenge.getError() != null) {
            return challenge.getError().getDetail();
        }
        return "no details";
    }

    public void update(String domainName, DnsZone dnsZone, SecretStore secretStore, Optional<WebApp> httpsWebAppToPushCert, boolean staging, String contactEmail) {

        logger.info("Will update certificate for domain {} with staging mode {}", domainName, staging);

        // Get or create accountKeypairPem from secret store
        logger.info("Get the account keypair from the secret store");
        String accountKeypairPem = cloudService.secretGetAsText(secretStore, "account", "keypairPem");
        if (accountKeypairPem == null) {
            logger.info("There is no account keypair in the secret store. Create one");
            AsymmetricKeys keys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);
            accountKeypairPem = RSACrypt.RSA_CRYPT.savePrivateKeyPemAsString(keys) + RSACrypt.RSA_CRYPT.savePublicKeyPemAsString(keys);
            cloudService.secretSetAsTextOrFail(secretStore, "account", "keypairPem", accountKeypairPem);
        }

        // Get recently stored certificate
        boolean updateCertswithLetsEncrypt = false;

        String secretNamespace = domainName + (staging ? "-staging" : "");
        logger.info("Get recent certificate from secret store");
        String certificatePem = cloudService.secretGetAsText(secretStore, secretNamespace, "certificatePem");
        String publicKeyPem = cloudService.secretGetAsText(secretStore, secretNamespace, "publicKeyPem");
        String privateKeyPem = cloudService.secretGetAsText(secretStore, secretNamespace, "privateKeyPem");
        String pfxPassword = cloudService.secretGetAsText(secretStore, secretNamespace, "pfxPassword");
        RSACertificate rsaCertificate = null;

        // If present, check if expire in 1 month
        if (CollectionsTools.isAllItemNotNull(certificatePem, publicKeyPem, privateKeyPem, pfxPassword)) {
            rsaCertificate = RSACertificate.loadPemFromString(certificatePem, publicKeyPem, privateKeyPem);
            logger.info("Has recent certificate {} with end date {}", rsaCertificate.getThumbprint(), DateTools.formatFull(rsaCertificate.getEndDate()));
            if (DateTools.isExpired(rsaCertificate.getEndDate(), Calendar.MONTH, -1)) {
                logger.info("Expires in less than 1 month. Will update");
                updateCertswithLetsEncrypt = true;
            }
        } else {
            logger.info("No recent certificate. Will update");
            updateCertswithLetsEncrypt = true;
        }

        // Update certs
        if (updateCertswithLetsEncrypt) {

            logger.info("Must update the cert from Let's Encrypt");

            String acmeUrl;
            if (staging) {
                acmeUrl = "acme://letsencrypt.org/staging";
            } else {
                acmeUrl = "acme://letsencrypt.org/";
            }

            try {
                // Acme Login
                logger.info("Logging to {}", acmeUrl);
                Session acmeSession = new Session(new URI(acmeUrl));
                KeyPair accountKeyPair = RSATools.createKeyPair(RSACrypt.RSA_CRYPT.loadKeysPemFromString(accountKeypairPem));

                logger.info("Registering account");
                Account acmeAccount = new AccountBuilder() //
                        .addContact("mailto:" + contactEmail) //
                        .agreeToTermsOfService() //
                        .useKeyPair(accountKeyPair) //
                        .create(acmeSession);

                URL accountLocationUrl = acmeAccount.getLocation();
                acmeSession.login(accountLocationUrl, accountKeyPair);

                // Get the location
                logger.info("AcmeClient location: {}", accountLocationUrl);

                // Get the challenge
                logger.info("Getting the challenge");

                Order order = acmeAccount.newOrder() //
                        .domains(domainName) //
                        .create();

                // Get the DNS challenge
                Dns01Challenge dnsChallenge = null;
                List<String> availableChallenges = new ArrayList<>();
                for (Authorization auth : order.getAuthorizations()) {
                    auth.getChallenges().stream().map(Challenge::getType).forEach(availableChallenges::add);
                    dnsChallenge = auth.findChallenge(Dns01Challenge.TYPE);
                }
                if (dnsChallenge == null) {
                    throw new CliException("DNS Challenge not found for " + domainName + " ; Available challenges are: [" + Joiner.on(", ").join(availableChallenges) + "]");
                }

                String digest = dnsChallenge.getDigest();

                // Add DNS Entry
                String challengeDnsDomain = "_acme-challenge." + domainName;
                RawDnsEntry rawDnsEntry = new RawDnsEntry() //
                        .setName(challengeDnsDomain) //
                        .setType("TXT") //
                        .setTtl(60) //
                        .setDetails(digest);
                cloudService.dnsSetEntry(dnsZone, rawDnsEntry);

                // Wait
                Lookup lookup = new Lookup(challengeDnsDomain, Type.TXT);
                while (true) {

                    Record[] records = lookup.run();
                    if (records == null || records.length == 0) {
                        logger.info("{} not yet visible on DNS. Wait 10 seconds", challengeDnsDomain);
                    } else {
                        boolean hasRightValue = Arrays.stream(records).flatMap(record -> ((TXTRecord) record).getStrings().stream()).anyMatch(it -> StringTools.safeEquals(digest, it));
                        if (hasRightValue) {
                            logger.info("{} is visible on DNS and with the right value. Continue", challengeDnsDomain);
                            break;
                        } else {
                            logger.info("{} is visible on DNS, but with the wrong value. Wait 10 seconds", challengeDnsDomain);
                        }
                    }

                    ThreadTools.sleep(10000);
                }

                // Complete the challenges
                logger.info("Triggering the challenge");
                dnsChallenge.trigger();
                // Wait until completed
                while (dnsChallenge.getStatus() != Status.VALID) {
                    if (dnsChallenge.getStatus() == Status.INVALID) {
                        throw new CliException("The challenge failed: " + getChallengeErrorDetails(dnsChallenge));
                    }

                    ThreadTools.sleep(5 * 1000); // 5 secs

                    logger.info("Updating the status");
                    dnsChallenge.update();

                    logger.info("Current status: {}", dnsChallenge.getStatus());
                }

                // Get the cert
                logger.info("Get the certificate from Lets Encrypt");
                AsymmetricKeys asymmetricKeys = RSACrypt.RSA_CRYPT.generateKeyPair(4096);

                CSRBuilder csrb = new CSRBuilder();
                csrb.addDomain(domainName);

                logger.info("Getting certificate for: {}", domainName);
                csrb.sign(RSATools.createKeyPair(asymmetricKeys));
                byte[] csr = csrb.getEncoded();

                order.execute(csr);

                // Wait the order to be ready
                int count = 0;
                while (order.getStatus() != Status.VALID && count < 6) {
                    if (order.getStatus() == Status.INVALID) {
                        throw new CliException("The order failed");
                    }
                    ThreadTools.sleep(10 * 1000); // 10 secs
                    try {
                        logger.info("Updating the status");
                        order.update();
                    } catch (AcmeException e) {
                        logger.error("Problem updating the order status", e);
                        throw new CliException("Problem updating the order status", e);
                    }
                    logger.info("[{}] Current order status: {}", count, order.getStatus());
                    ++count;
                }

                if (order.getStatus() != Status.VALID) {
                    logger.error("Order status is still not valid after 1 minute. Status is {} ; problem is {} ; json: {}", order.getStatus(), order.getError(), order.getJSON().toString());
                    throw new CliException("Order status is still not valid after 1 minute. Status is " + order.getStatus());
                }

                Certificate certificate = order.getCertificate();
                X509Certificate cert = certificate.getCertificate();

                rsaCertificate = new RSACertificate(cert);
                rsaCertificate.setKeysForSigning(asymmetricKeys);

                logger.info("Successfully updated certificate: {}", domainName);

            } catch (CliException e) {
                throw e;
            } catch (Exception e) {
                throw new CliException("Problem using ACME", e);
            }

            // Store the cert
            certificatePem = rsaCertificate.saveCertificatePemAsString();
            publicKeyPem = RSACrypt.RSA_CRYPT.savePublicKeyPemAsString(rsaCertificate.getKeysForSigning());
            privateKeyPem = RSACrypt.RSA_CRYPT.savePrivateKeyPemAsString(rsaCertificate.getKeysForSigning());
            pfxPassword = SecureRandomTools.randomHexString(10);

            cloudService.secretSetAsTextOrFail(secretStore, secretNamespace, "caCertificatePem", CA_CERTIFICATE_TEXT_PEM);
            cloudService.secretSetAsTextOrFail(secretStore, secretNamespace, "certificatePem", certificatePem);
            cloudService.secretSetAsTextOrFail(secretStore, secretNamespace, "publicKeyPem", publicKeyPem);
            cloudService.secretSetAsTextOrFail(secretStore, secretNamespace, "privateKeyPem", privateKeyPem);
            cloudService.secretSetAsTextOrFail(secretStore, secretNamespace, "pfxPassword", pfxPassword);

        }

        // Push certificate
        if (httpsWebAppToPushCert.isPresent()) {
            RSACertificate caRsaCertificate = RSACertificate.loadPemFromString(CA_CERTIFICATE_TEXT_PEM);
            WebApp httpsWebApp = httpsWebAppToPushCert.get();
            logger.info("Push certificate on {}", httpsWebApp);
            cloudService.pushCertificate(domainName, httpsWebApp, caRsaCertificate, rsaCertificate, pfxPassword);
        }

    }

}
