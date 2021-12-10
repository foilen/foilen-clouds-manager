/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.pkcs.PKCS12PfxPduBuilder;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.bc.BcPKCS12MacCalculatorBuilder;
import org.bouncycastle.pkcs.bc.BcPKCS12PBEOutputEncryptorBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS12SafeBagBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.SslState;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.authorization.models.ActiveDirectoryUser;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.dns.fluent.models.RecordSetInner;
import com.azure.resourcemanager.dns.models.CnameRecord;
import com.azure.resourcemanager.dns.models.DnsZone;
import com.azure.resourcemanager.keyvault.models.Secret;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.AzureConstants;
import com.foilen.clouds.manager.services.model.AzureDnsZone;
import com.foilen.clouds.manager.services.model.AzureKeyVault;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.ResourcesBucket;
import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.clouds.manager.services.model.json.AzSubscription;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSATools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.StringTools;

@Component
public class CloudAzureService extends AbstractBasics {

    private static final int ITERATION_COUNT = 2048;

    protected static AzProfileDetails getAzureProfile(String azureProfileFile) {
        return JsonTools.readFromFile(azureProfileFile, AzProfileDetails.class);
    }

    @Autowired
    private ResourcesBucketService resourcesBucketService;
    private AzureProfile profile;
    private TokenCredential tokenCredential;
    private AzureResourceManager azureResourceManager;

    private String userName;

    public void discoverAll() {

        init();

        ResourcesBucket resourcesBucket = new ResourcesBucket();

        dnsZonesDiscoverAll(resourcesBucket);
        webAppServicesDiscoverAll(resourcesBucket);
        keyVaultsDiscoverAll(resourcesBucket);

        resourcesBucketService.setAzureResourcesBucket(resourcesBucket);

    }

    public List<RawDnsEntry> dnsListEntries(AzureDnsZone azureDnsZone) {

        init();

        logger.info("Get DNS Zone {}", azureDnsZone.getId());
        DnsZone dnsZone = azureResourceManager.dnsZones().getById(azureDnsZone.getId());

        List<RawDnsEntry> rawDnsEntries = new ArrayList<>();
        dnsZone.listRecordSets().forEach(record -> {

            RecordSetInner innerModel = record.innerModel();
            if (innerModel.aaaaRecords() != null) {
                innerModel.aaaaRecords().forEach(r -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.ipv6Address()) //
                        .setTtl(record.timeToLive()) //
                ));
            }
            if (innerModel.aRecords() != null) {
                innerModel.aRecords().forEach(r -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.ipv4Address()) //
                        .setTtl(record.timeToLive()) //
                ));
            }
            if (innerModel.cnameRecord() != null) {
                CnameRecord r = innerModel.cnameRecord();
                rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.cname()) //
                        .setTtl(record.timeToLive()) //
                );
            }
            if (innerModel.mxRecords() != null) {
                innerModel.mxRecords().forEach(r -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.exchange()) //
                        .setPriority(r.preference()) //
                        .setTtl(record.timeToLive()) //
                ));
            }
            if (innerModel.nsRecords() != null) {
                innerModel.nsRecords().forEach(r -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.nsdname()) //
                        .setTtl(record.timeToLive()) //
                ));
            }
            if (innerModel.srvRecords() != null) {
                innerModel.srvRecords().forEach(r -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(r.target()) //
                        .setPriority(r.priority()) //
                        .setWeight(r.weight()) //
                        .setPort(r.port()) //
                        .setTtl(record.timeToLive()) //
                ));
            }
            if (innerModel.txtRecords() != null) {
                innerModel.txtRecords().forEach(r -> {
                    r.value().forEach(v -> {
                        rawDnsEntries.add(new RawDnsEntry() //
                                .setName(trimDot(record.fqdn())) //
                                .setType(record.recordType().name()) //
                                .setDetails(v) //
                                .setTtl(record.timeToLive()) //
                        );
                    });
                });
            }
        });

        return rawDnsEntries;
    }

    public void dnsSetEntry(AzureDnsZone azureDnsZone, RawDnsEntry rawDnsEntry) {

        init();

        logger.info("Get DNS Zone {}", azureDnsZone.getId());
        DnsZone dnsZone = azureResourceManager.dnsZones().getById(azureDnsZone.getId());

        logger.info("Set {}", rawDnsEntry);
        String baseDomainName = azureDnsZone.getName();
        String subDomain = rawDnsEntry.getName();
        subDomain = subDomain.substring(0, subDomain.length() - 1 - baseDomainName.length());
        switch (rawDnsEntry.getType()) {
        case "AAAA":
            dnsZone.update() //
                    .withoutAaaaRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineAaaaRecordSet(subDomain) //
                    .withIPv6Address(rawDnsEntry.getDetails()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "A":
            dnsZone.update() //
                    .withoutARecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineARecordSet(subDomain) //
                    .withIPv4Address(rawDnsEntry.getDetails()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "CNAME":
            dnsZone.update() //
                    .withoutCNameRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineCNameRecordSet(subDomain) //
                    .withAlias(rawDnsEntry.getDetails()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "MX":
            dnsZone.update() //
                    .withoutMXRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineMXRecordSet(subDomain) //
                    .withMailExchange(rawDnsEntry.getDetails(), rawDnsEntry.getPriority()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "NS":
            dnsZone.update() //
                    .withoutNSRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineNSRecordSet(subDomain) //
                    .withNameServer(rawDnsEntry.getDetails()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "SRV":
            dnsZone.update() //
                    .withoutSrvRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineSrvRecordSet(subDomain) //
                    .withRecord(subDomain, rawDnsEntry.getPort(), rawDnsEntry.getPriority(), rawDnsEntry.getWeight()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        case "TXT":
            dnsZone.update() //
                    .withoutTxtRecordSet(subDomain) //
                    .apply();
            dnsZone.update() //
                    .defineTxtRecordSet(subDomain) //
                    .withText(rawDnsEntry.getDetails()) //
                    .withTimeToLive(rawDnsEntry.getTtl()) //
                    .attach() //
                    .apply();
            break;

        }

    }

    private void dnsZonesDiscoverAll(ResourcesBucket resourcesBucket) {

        init();

        logger.info("DNS Zones");
        PagedIterable<DnsZone> dnsZones = azureResourceManager.dnsZones().list();
        dnsZones.forEach(dnsZone -> {
            logger.info("DNS Zone [{}] : {}", dnsZone.name(), dnsZone.id());
            dnsZone.listRecordSets().forEach(record -> {
                String domain = record.fqdn();
                while (domain.endsWith(".")) {
                    domain = domain.substring(0, domain.length() - 1);
                }
                logger.info("DNS Zone [{}] Record : {}", dnsZone.name(), domain);
                resourcesBucket.addDomain(domain, AzureDnsZone.from(dnsZone));
            });

        });
    }

    private void init() {

        if (profile == null) {
            logger.info("Prepare profile");

            String azureProfileFile = JavaEnvironmentValues.getHomeDirectory() + "/.azure/azureProfile.json";
            if (FileTools.exists(azureProfileFile)) {
                AzProfileDetails azProfileDetails = getAzureProfile(azureProfileFile);
                if (!azProfileDetails.getSubscriptions().isEmpty()) {
                    AzSubscription subscription = azProfileDetails.getSubscriptions().get(0);
                    String tenantId = subscription.getTenantId();
                    userName = subscription.getUser().getName();
                    logger.info("Using tenant id {} and user name {}", tenantId, userName);
                    profile = new AzureProfile(tenantId, null, AzureEnvironment.AZURE);
                }
            }
        }
        if (profile == null) {
            logger.info("Prepare profile");
            profile = new AzureProfile(AzureEnvironment.AZURE);
        }

        if (tokenCredential == null) {
            logger.info("Prepare token credential");
            tokenCredential = new DefaultAzureCredentialBuilder() //
                    .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()) //
                    .build();
        }

        if (azureResourceManager == null) {
            logger.info("Prepare resource manager");
            azureResourceManager = AzureResourceManager.configure() //
                    .withLogLevel(HttpLogDetailLevel.BASIC) //
                    .authenticate(tokenCredential, profile) //
                    .withDefaultSubscription();
        }

    }

    public AzureKeyVault keyVaultCreate(String keyVaultName, String regionName, String resourceGroup) {

        init();

        // Create the vault
        logger.info("Create vault {}", keyVaultName);
        Vault vault = azureResourceManager.vaults().define(keyVaultName) //
                .withRegion(regionName) //
                .withExistingResourceGroup(resourceGroup) //
                .withRoleBasedAccessControl() //
                .create();

        AzureKeyVault azureKeyVault = AzureKeyVault.from(vault);

        resourcesBucketService.updateAzureResourcesBucket(resourcesBucket -> {
            resourcesBucket.addSecretStore(resourceGroup, azureKeyVault);
        });

        // Find current user
        logger.info("Find user {}", userName);
        ActiveDirectoryUser user = azureResourceManager.accessManagement() //
                .activeDirectoryUsers() //
                .getByName(userName);

        // Add permission
        try {
            logger.info("Create admin permission to vault {} for user {} -> {}", keyVaultName, userName, user.id());
            azureResourceManager.accessManagement().roleAssignments() //
                    .define(UUID.randomUUID().toString()) //
                    .forUser(user) //
                    .withBuiltInRole(BuiltInRole.KEY_VAULT_ADMINISTRATOR) //
                    .withResourceScope(vault) //
                    .create();
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.ROLE_ASSIGNMENT_EXISTS)) {
                logger.info("Admin permission to vault {} for user {} -> {} already exists", keyVaultName, userName, user.id());
            } else {
                throw e;
            }
        }

        return azureKeyVault;
    }

    private void keyVaultsDiscoverAll(ResourcesBucket resourcesBucket) {

        init();

        logger.info("Resource Groups");
        PagedIterable<ResourceGroup> resourceGroups = azureResourceManager.resourceGroups().list();
        resourceGroups.forEach(resourceGroup -> {
            String resourceGroupName = resourceGroup.name();
            logger.info("Resource Group [{}] : {}", resourceGroupName, resourceGroup.id());

            logger.info("Vault in Resource Group {}", resourceGroupName);
            PagedIterable<Vault> vaults = azureResourceManager.vaults().listByResourceGroup(resourceGroupName);
            vaults.forEach(vault -> {
                logger.info("Vault [{}] : {}", vault.name(), vault.id());
                resourcesBucket.addSecretStore(resourceGroupName, AzureKeyVault.from(vault));
            });

        });

    }

    public String keyVaultSecretGetAsText(AzureKeyVault azureKeyVault, String secretName) {

        init();

        secretName = sanitizedSecretName(secretName);

        logger.info("Get vault {}", azureKeyVault.getId());

        Vault vault = azureResourceManager.vaults() //
                .getById(azureKeyVault.getId());

        logger.info("Get secret {}", secretName);

        try {
            return vault.secrets().getByName(secretName).getValue();
        } catch (ResourceNotFoundException e) {
            logger.info("Secret {} doesn't exist", azureKeyVault.getId());
            return null;
        }
    }

    public PagedIterable<Secret> keyVaultSecretList(AzureKeyVault azureKeyVault) {

        init();

        logger.info("Get vault {}", azureKeyVault.getId());

        Vault vault = azureResourceManager.vaults() //
                .getById(azureKeyVault.getId());

        logger.info("Get secrets list");

        return vault.secrets().list();

    }

    public void keyVaultSecretSetAsTextOrFail(AzureKeyVault azureKeyVault, String secretName, String value) {

        init();

        secretName = sanitizedSecretName(secretName);

        logger.info("Get vault {}", azureKeyVault.getId());

        Vault vault = azureResourceManager.vaults() //
                .getById(azureKeyVault.getId());

        logger.info("Set secret {}", secretName);

        try {
            vault.secrets().define(secretName).withValue(value).create();
        } catch (Exception e) {
            logger.error("Could not set secret {}", azureKeyVault.getId(), e);
        }
    }

    private String sanitizedSecretName(String secretName) {
        return secretName.replace('.', '-');
    }

    private String trimDot(String name) {
        while (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }

    public void webAppServicePushCertificate(String hostname, AzureWebApp azureWebApp, RSACertificate caRsaCertificate, RSACertificate hostRsaCertificate, String pfxPassword) {

        init();

        logger.info("Generate PFX certificate");
        byte[] pfx;
        try {
            JcaX509ExtensionUtils jcaX509ExtensionUtils = new JcaX509ExtensionUtils();

            PublicKey publicKey = RSATools.createPublicKey(hostRsaCertificate.getKeysForSigning());
            PrivateKey privateKey = RSATools.createPrivateKey(hostRsaCertificate.getKeysForSigning());

            pfx = new PKCS12PfxPduBuilder() //
                    // Host Cert
                    .addData(new JcaPKCS12SafeBagBuilder(hostRsaCertificate.getCertificate()) //
                            .addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(hostRsaCertificate.getCommonName())) //
                            .build() //
                    )
                    // CA cert
                    .addData(new JcaPKCS12SafeBagBuilder(caRsaCertificate.getCertificate()) //
                            .addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(caRsaCertificate.getCommonName())) //
                            .build() //
                    )
                    // Private Key
                    .addEncryptedData( //
                            new BcPKCS12PBEOutputEncryptorBuilder( //
                                    PKCSObjectIdentifiers.pbeWithSHAAnd40BitRC2_CBC, //
                                    new CBCBlockCipher(new RC2Engine()) //
                            ) //
                                    .setIterationCount(2048) //
                                    .build(pfxPassword.toCharArray()),
                            new PKCS12SafeBag[] { //
                                    new JcaPKCS12SafeBagBuilder( //
                                            privateKey, //
                                            new BcPKCS12PBEOutputEncryptorBuilder( //
                                                    PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, //
                                                    new CBCBlockCipher(new DESedeEngine()) //
                                            ) //
                                                    .setIterationCount(2048) //
                                                    .build(pfxPassword.toCharArray()) //
                                    ) //
                                            .addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString(hostRsaCertificate.getCommonName())) //
                                            .addBagAttribute(PKCSObjectIdentifiers.pkcs_9_at_localKeyId, jcaX509ExtensionUtils.createSubjectKeyIdentifier(publicKey)) //
                                            .build() //
                            } //
                    ) //
                    .build(new BcPKCS12MacCalculatorBuilder().setIterationCount(ITERATION_COUNT), pfxPassword.toCharArray()) //
                    .getEncoded();

        } catch (Exception e) {
            throw new CliException("Problem saving to PKCS12", e);
        }

        logger.info("Bind certificate {} to web app {}", hostRsaCertificate.getThumbprint(), azureWebApp.getId());
        File pfxFile;
        try {
            pfxFile = File.createTempFile("cert", ".pfx");
            FileTools.writeFile(pfx, pfxFile);
        } catch (IOException e) {
            throw new CliException("Problem creating pfx file", e);
        }
        azureResourceManager.webApps().getById(azureWebApp.getId()) //
                .update() //
                .defineSslBinding().forHostname(hostname).withPfxCertificateToUpload(pfxFile, pfxPassword).withSniBasedSsl().attach() //
                .apply();
        pfxFile.delete();

    }

    private void webAppServicesDiscoverAll(ResourcesBucket resourcesBucket) {

        init();

        logger.info("Web App Services");

        PagedIterable<WebAppBasic> webApps = azureResourceManager.webApps().list();
        webApps.forEach(webApp -> {
            logger.info("WebApp [{}] : {}", webApp.name(), webApp.id());

            webApp.hostnameSslStates().forEach((hostname, state) -> {
                boolean ssl = state.sslState() != SslState.DISABLED;
                logger.info("WebApp [{}] Domain : {} ; Ssl : {}", webApp.name(), hostname, ssl);
                resourcesBucket.addDomain(hostname, AzureWebApp.from(webApp), ssl);
            });
        });

    }
}
