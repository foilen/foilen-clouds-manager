/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.azure.core.credential.TokenCredential;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.OperatingSystem;
import com.azure.resourcemanager.appservice.models.PricingTier;
import com.azure.resourcemanager.authorization.models.ActiveDirectoryUser;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.dns.fluent.models.RecordSetInner;
import com.azure.resourcemanager.dns.models.CnameRecord;
import com.azure.resourcemanager.dns.models.DnsZone;
import com.azure.resourcemanager.keyvault.models.Secret;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.mariadb.MariaDBManager;
import com.azure.resourcemanager.mariadb.models.*;
import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.ManageUnrecoverableException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.*;
import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.clouds.manager.services.model.json.AzSubscription;
import com.foilen.databasetools.connection.JdbcUriConfigConnection;
import com.foilen.databasetools.manage.mariadb.MariadbManageProcess;
import com.foilen.databasetools.manage.mariadb.MariadbManagerConfig;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSATools;
import com.foilen.smalltools.tools.*;
import com.google.common.base.Strings;
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
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CloudAzureService extends AbstractBasics {

    private static final int ITERATION_COUNT = 2048;
    private AzureProfile profile;
    private TokenCredential tokenCredential;
    private AzureResourceManager azureResourceManager;
    private String defaultSubscriptionId;
    private String userName;

    protected static AzProfileDetails getAzureProfile(String azureProfileFile) {
        return JsonTools.readFromFile(azureProfileFile, AzProfileDetails.class);
    }

    public List<AzureApplicationServicePlan> applicationServicePlansFindAll() {

        init();

        return azureResourceManager.appServicePlans().list().stream()
                .map(AzureApplicationServicePlan::from)
                .collect(Collectors.toList());

    }

    public Optional<AzureApplicationServicePlan> applicationServicePlanFindByName(String resourceGroupName, String applicationServicePlanName) {

        init();

        logger.info("Get Application farm {} / {}", resourceGroupName, applicationServicePlanName);
        try {
            var appServicePlan = azureResourceManager.appServicePlans().getByResourceGroup(resourceGroupName, applicationServicePlanName);
            return Optional.of(AzureApplicationServicePlan.from(appServicePlan));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }

    }

    public AzureApplicationServicePlan applicationServicePlanManage(ManageConfiguration config, AzureApplicationServicePlan desired) {

        AssertTools.assertNotNull(desired.getName(), "name must be provided");

        if (Strings.isNullOrEmpty(desired.getResourceGroup())) {
            fillResourceGroup(config, desired);
        }
        if (Strings.isNullOrEmpty(desired.getRegion())) {
            fillRegion(config, desired);
        }

        AssertTools.assertNotNull(desired.getResourceGroup(), "resource group must be provided");
        AssertTools.assertNotNull(desired.getRegion(), "region must be provided");

        var pricingTier = PricingTier.getAll().stream()
                .filter(it -> StringTools.safeEquals(it.toSkuDescription().size(), desired.getPricingTierSize()))
                .findFirst().orElse(PricingTier.FREE_F1);
        var operatingSystem = OperatingSystem.fromString(desired.getOperatingSystem());
        if (operatingSystem == null) {
            operatingSystem = OperatingSystem.LINUX;
        }
        desired.setOperatingSystem(operatingSystem.name());
        if (desired.getCapacity() == null) {
            desired.setCapacity(1);
        }
        if (desired.getPerSiteScaling() == null) {
            desired.setPerSiteScaling(true);
        }

        logger.info("Check {}", desired);
        var current = applicationServicePlanFindByName(desired.getResourceGroup(), desired.getName()).orElse(null);
        if (current == null) {
            // create
            logger.info("Create: {}", desired);
            current = AzureApplicationServicePlan.from(azureResourceManager.appServicePlans().define(desired.getName())
                    .withRegion(desired.getRegion())
                    .withExistingResourceGroup(desired.getResourceGroup())
                    .withPricingTier(pricingTier)
                    .withOperatingSystem(operatingSystem)
                    .withCapacity(desired.getCapacity())
                    .withPerSiteScaling(desired.getPerSiteScaling())
                    .create());
        } else {
            // Check
            logger.info("Exists: {}", desired);

            var differences = desired.differences(current);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        return current;

    }

    public Optional<com.foilen.clouds.manager.services.model.DnsZone> dnsFindById(String azureDnsZoneId) {

        init();

        logger.info("Get DNS Zone {}", azureDnsZoneId);
        try {
            DnsZone dnsZone = azureResourceManager.dnsZones().getById(azureDnsZoneId);
            return Optional.of(AzureDnsZone.from(dnsZone));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }

    }

    public List<AzureDnsZone> dnsZoneList() {

        init();

        logger.info("List DNS Zones");
        var dnsZoneIt = azureResourceManager.dnsZones().list();
        return dnsZoneIt.stream()
                .map(AzureDnsZone::from)
                .sorted((a, b) -> StringTools.safeComparisonNullFirst(a.getName(), b.getName()))
                .collect(Collectors.toList());

    }

    public Optional<AzureDnsZone> dnsZoneFindByName(String resourceGroupName, String dnsZoneName) {

        init();

        logger.info("Get DNS Zone {} / {}", resourceGroupName, dnsZoneName);
        try {
            DnsZone dnsZone = azureResourceManager.dnsZones().getByResourceGroup(resourceGroupName, dnsZoneName);
            return Optional.of(AzureDnsZone.from(dnsZone));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }

    }

    public List<RawDnsEntry> dnsZoneEntryList(AzureDnsZone azureDnsZone) {

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

        Collections.sort(rawDnsEntries);
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
                    defaultSubscriptionId = subscription.getId();
                    logger.info("Using tenant id {} and user name {}", tenantId, userName);
                    profile = new AzureProfile(tenantId, defaultSubscriptionId, AzureEnvironment.AZURE);
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

    public AzureKeyVault keyVaultCreate(String resourceGroupName, Optional<String> regionName, String keyVaultName) {

        init();

        // Get the resource group's region if not provided
        if (regionName.isEmpty()) {
            logger.info("Region not provided. Getting the one for the resource group {}", resourceGroupName);
            var resourceGroup = azureResourceManager.resourceGroups().getByName(resourceGroupName);
            regionName = Optional.of(resourceGroup.regionName());
            logger.info("Will use region {}", regionName.get());
        }

        // Create the vault
        logger.info("Create vault {}", keyVaultName);
        Vault vault = azureResourceManager.vaults().define(keyVaultName) //
                .withRegion(regionName.get()) //
                .withExistingResourceGroup(resourceGroupName) //
                .withRoleBasedAccessControl() //
                .create();

        AzureKeyVault azureKeyVault = AzureKeyVault.from(vault);

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

    public List<AzureKeyVault> keyVaultFindAll() {

        init();

        return azureResourceManager.resourceGroups().list().stream()
                .flatMap(resourceGroup -> azureResourceManager.vaults().listByResourceGroup(resourceGroup.name()).stream())
                .map(AzureKeyVault::from)
                .collect(Collectors.toList());
    }

    public Optional<AzureKeyVault> keyVaultFindByName(String resourceGroupName, String keyVaultName) {

        init();

        try {
            var resource = azureResourceManager.vaults().getByResourceGroup(resourceGroupName, keyVaultName);
            return Optional.of(AzureKeyVault.from(resource));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<SecretStore> keyVaultFindById(String azureKeyVaultId) {

        init();

        try {
            var resource = azureResourceManager.vaults().getById(azureKeyVaultId);
            return Optional.of(AzureKeyVault.from(resource));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    public AzureKeyVault keyVaultManage(ManageConfiguration config, AzureKeyVault desired) {

        AssertTools.assertNotNull(desired.getName(), "name must be provided");

        if (Strings.isNullOrEmpty(desired.getResourceGroup())) {
            fillResourceGroup(config, desired);
        }
        if (Strings.isNullOrEmpty(desired.getRegion())) {
            fillRegion(config, desired);
        }

        AssertTools.assertNotNull(desired.getResourceGroup(), "resource group must be provided");
        AssertTools.assertNotNull(desired.getRegion(), "region must be provided");

        logger.info("Check {}", desired);
        var current = keyVaultFindByName(desired.getResourceGroup(), desired.getName()).orElse(null);
        if (current == null) {
            // create
            logger.info("Create: {}", desired);
            current = keyVaultCreate(desired.getResourceGroup(), Optional.of(desired.getRegion()), desired.getName());
        } else {
            // Check
            logger.info("Exists: {}", desired);

            var differences = desired.differences(current);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        return current;

    }

    public List<AzureMariadb> mariadbList() {

        init();

        logger.info("List MariaDB databases");
        var manager = MariaDBManager.authenticate(tokenCredential, profile);
        return manager.servers().list().stream()
                .map(AzureMariadb::from)
                .sorted((a, b) -> StringTools.safeComparisonNullFirst(a.getName(), b.getName()))
                .collect(Collectors.toList());

    }

    public Optional<AzureMariadb> mariadbFindById(String mariadbId) {

        init();

        logger.info("Get MariaDB database {}", mariadbId);
        try {
            var manager = MariaDBManager.authenticate(tokenCredential, profile);
            var server = manager.servers().getById(mariadbId);
            return Optional.of(AzureMariadb.from(server));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public Optional<AzureMariadb> mariadbFindByName(String resourceGroupName, String mariadbName) {

        init();

        logger.info("Get MariaDB database {} / {}", resourceGroupName, mariadbName);
        try {
            var manager = MariaDBManager.authenticate(tokenCredential, profile);
            var server = manager.servers().getByResourceGroup(resourceGroupName, mariadbName);
            return Optional.of(AzureMariadb.from(server));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public AzureMariadb mariadbManage(ManageConfiguration config, AzureMariadbManageConfiguration desired) {

        var desiredResource = desired.getResource();

        AssertTools.assertNotNull(desiredResource, "resource must be provided");
        AssertTools.assertNotNull(desiredResource.getName(), "resource.name must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegion())) {
            fillRegion(config, desiredResource);
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource.resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegion(), "resource.region must be provided");

        logger.info("Check {}", desired);
        var currentResource = mariadbFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desired);
            var manager = MariaDBManager.authenticate(tokenCredential, profile);
            currentResource = AzureMariadb.from(manager.servers().define(desiredResource.getName())
                    .withRegion(desiredResource.getRegion())
                    .withExistingResourceGroup(desiredResource.getResourceGroup())
                    .withProperties(new ServerPropertiesForDefaultCreate()
                            .withAdministratorLogin(desiredResource.getAdministratorLogin())
                            .withAdministratorLoginPassword(desiredResource.getAdministratorLoginPassword())
                            .withSslEnforcement(SslEnforcementEnum.fromString(desiredResource.getSslEnforcement()))
                            .withMinimalTlsVersion(MinimalTlsVersionEnum.fromString(desiredResource.getMinimalTlsVersion()))
                            .withStorageProfile(desiredResource.getStorageProfile())
                            .withPublicNetworkAccess(PublicNetworkAccessEnum.fromString(desiredResource.getPublicNetworkAccess()))
                            .withVersion(ServerVersion.fromString(desiredResource.getVersion()))
                    )
                    .withSku(new Sku().withName(desiredResource.getSkuName()))
                    .create());
        } else {
            // Check
            logger.info("Exists: {}", desired);

            var differences = desiredResource.differences(currentResource);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        // Apply databases and users if provided
        if (desired.getConfig() != null) {
            String name = desiredResource.getName();
            String administratorLogin = desiredResource.getAdministratorLogin();
            String administratorLoginPassword = desiredResource.getAdministratorLoginPassword();

            logger.info("Managing databases and users on: {}", currentResource.getId());
            File manageConfigFile = null;
            try {
                manageConfigFile = File.createTempFile("manage", ".json");
                var jdbcUri = "jdbc:mariadb://" + name + ".mariadb.database.azure.com:3306/mysql?useSSL=true&user=" + administratorLogin + "@" + name + "&password=" + administratorLoginPassword;

                MariadbManagerConfig mariadbManagerConfig = desired.getConfig();
                mariadbManagerConfig.setConnection(new JdbcUriConfigConnection().setJdbcUri(jdbcUri));
                JsonTools.writeToFile(manageConfigFile, mariadbManagerConfig);

                MariadbManageProcess mariadbManageProcess = new MariadbManageProcess(manageConfigFile.getAbsolutePath(), false);
                mariadbManageProcess.run();

            } catch (Exception e) {
                throw new ManageUnrecoverableException("Problem while managing databases and users", e);
            } finally {
                if (manageConfigFile != null) {
                    manageConfigFile.delete();
                }
            }
        }

        return currentResource;

    }

    public List<AzureResourceGroup> resourceGroupFindAll() {

        init();

        return azureResourceManager.resourceGroups().list().stream()
                .map(AzureResourceGroup::from)
                .collect(Collectors.toList());
    }

    public Optional<AzureResourceGroup> resourceGroupFindByName(String resourceGroupName) {

        init();

        try {
            var resource = azureResourceManager.resourceGroups().getByName(resourceGroupName);
            return Optional.of(AzureResourceGroup.from(resource));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_GROUP_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }
    }

    public AzureResourceGroup resourceGroupManage(AzureResourceGroup desired) {
        AssertTools.assertNotNull(desired.getName(), "name must be provided");
        AssertTools.assertNotNull(desired.getRegion(), "region must be provided");

        logger.info("Check: {}", desired);
        var current = resourceGroupFindByName(desired.getName()).orElse(null);
        if (current == null) {
            // create
            logger.info("Create: {}", desired);
            current = AzureResourceGroup.from(azureResourceManager.resourceGroups().define(desired.getName())
                    .withRegion(desired.getRegion())
                    .create());
        } else {
            // Check
            logger.info("Exists: {}", desired);

            var differences = desired.differences(current);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        return current;

    }

    public Optional<WebApp> webappFindById(String azureWebappId) {

        init();

        try {
            var resource = azureResourceManager.webApps().getById(azureWebappId);
            return Optional.of(AzureWebApp.from(resource));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
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
                            new PKCS12SafeBag[]{ //
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

    private void fillRegion(ManageConfiguration config, HasRegion hasRegion) {
        if (config.getAzureResourceGroups().size() == 1) {
            hasRegion.setRegion(config.getAzureResourceGroups().get(0).getRegion());
        }
    }

    private void fillResourceGroup(ManageConfiguration config, HasResourceGroup hasResourceGroup) {
        if (config.getAzureResourceGroups().size() == 1) {
            hasResourceGroup.setResourceGroup(config.getAzureResourceGroups().get(0).getName());
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

}
