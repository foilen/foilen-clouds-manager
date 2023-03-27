/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.AzureNamedKeyCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.exception.ResourceNotFoundException;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.exception.ManagementException;
import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.Context;
import com.azure.identity.AzureCliCredentialBuilder;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.WebApp;
import com.azure.resourcemanager.appservice.models.*;
import com.azure.resourcemanager.authorization.models.ActiveDirectoryUser;
import com.azure.resourcemanager.authorization.models.BuiltInRole;
import com.azure.resourcemanager.dns.fluent.models.RecordSetInner;
import com.azure.resourcemanager.dns.models.CnameRecord;
import com.azure.resourcemanager.dns.models.DnsZone;
import com.azure.resourcemanager.keyvault.models.Secret;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.mariadb.MariaDBManager;
import com.azure.resourcemanager.mariadb.models.*;
import com.azure.resourcemanager.storage.models.SkuName;
import com.azure.resourcemanager.storage.models.StorageAccountSkuType;
import com.azure.storage.file.share.ShareDirectoryClient;
import com.azure.storage.file.share.ShareFileClientBuilder;
import com.azure.storage.file.share.ShareServiceClient;
import com.azure.storage.file.share.ShareServiceClientBuilder;
import com.azure.storage.file.share.models.ShareAccessTier;
import com.azure.storage.file.share.models.ShareFileItem;
import com.azure.storage.file.share.models.ShareProtocols;
import com.azure.storage.file.share.options.ShareCreateOptions;
import com.azure.storage.file.share.options.ShareListFilesAndDirectoriesOptions;
import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.ManageUnrecoverableException;
import com.foilen.clouds.manager.azureclient.AzureCustomClient;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.*;
import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.clouds.manager.services.model.json.AzSubscription;
import com.foilen.clouds.manager.services.model.manageconfig.*;
import com.foilen.databasetools.connection.JdbcUriConfigConnection;
import com.foilen.databasetools.manage.mariadb.MariadbManageProcess;
import com.foilen.databasetools.manage.mariadb.MariadbManagerConfig;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSATools;
import com.foilen.smalltools.listscomparator.ListComparatorHandler;
import com.foilen.smalltools.listscomparator.ListsComparator;
import com.foilen.smalltools.tools.*;
import com.foilen.smalltools.tuple.Tuple2;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xbill.DNS.ARecord;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CloudAzureService extends AbstractBasics {

    private static final Logger logger = LoggerFactory.getLogger(CloudAzureService.class);

    private static final int ITERATION_COUNT = 2048;

    @Autowired
    private AzureCustomClient azureCustomClient;

    private final Cache<String, String> storageAccountKeyCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    private static String storageAccountKeyCacheKey(String resourceGroupName, String storageAccountName) {
        return resourceGroupName + "|" + storageAccountName;
    }

    private AzureProfile profile;
    private TokenCredential tokenCredential;
    private AzureResourceManager azureResourceManager;
    private String userName;

    private LoadingCache<String, String> tokenCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
        @Override
        public String load(String scope) {
            init();
            TokenRequestContext request = new TokenRequestContext();
            request.addScopes(scope);
            return tokenCredential.getToken(request).block().getToken();
        }
    });

    protected static AzProfileDetails getAzureProfile(String azureProfileFile) {
        return JsonTools.readFromFile(azureProfileFile, AzProfileDetails.class);
    }

    protected List<RawDnsEntry> computeDnsEntries(ManageContext context, String domainName, List<RawDnsEntry> currentRawDnsEntries, DnsConfig desiredDnsConfig) {
        // Copy all the current entries if desired
        Map<String, Set<RawDnsEntry>> desiredEntriesByNameType = new HashMap<>();
        if (desiredDnsConfig.isStartEmpty()) {
            if (desiredDnsConfig.getStartWithDomains() != null) {
                currentRawDnsEntries.stream()
                        .filter(it -> desiredDnsConfig.getStartWithDomains().contains(it.getName()))
                        .forEach(it -> {
                            String nameType = it.getName() + "|" + it.getType();
                            CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                                    .add(it);
                        });
            }
        } else {
            currentRawDnsEntries.forEach(it -> {
                String nameType = it.getName() + "|" + it.getType();
                CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                        .add(it);
            });
        }

        // Compute the desired entries
        List<DnsEntryConfig> configs = desiredDnsConfig.getConfigs() == null ? Collections.emptyList() : desiredDnsConfig.getConfigs();
        for (var configEntries : configs) {

            // Raw entries
            if (configEntries.getRawDnsEntries() != null) {
                applyRawDnsEntriesOnMap(desiredEntriesByNameType, configEntries.getConflictResolution(), configEntries.getRawDnsEntries());
            }

            // Clear entries
            if (configEntries.getClearDnsEntries() != null) {
                configEntries.getClearDnsEntries().forEach(it -> {
                    logger.info("Clear entries of name {} and type {}", it.getName(), it.getType());
                    String nameType = it.getName() + "|" + it.getType();
                    desiredEntriesByNameType.remove(nameType);
                });
            }

            // Azure UID
            if (configEntries.getAzureUidDnsEntry() != null) {

                List<RawDnsEntry> rawDnsEntries = new ArrayList<>();
                for (var entry : configEntries.getAzureUidDnsEntry()) {
                    var webApp = webappFindByName(entry.getWebappResourceGroupName(), entry.getWebappName());
                    if (webApp.isEmpty()) {
                        context.needsNextStage(entry.getWebappResourceGroupName(), entry.getWebappName());
                        continue;
                    }

                    var azureWebSite = azureCustomClient.website(webApp.get().getId());
                    if (azureWebSite == null) {
                        context.needsNextStage(entry.getWebappResourceGroupName(), entry.getWebappName());
                        continue;
                    }

                    var value = azureWebSite.getProperties().getCustomDomainVerificationId();
                    rawDnsEntries.add(new RawDnsEntry()
                            .setName("asuid." + domainName)
                            .setType("TXT")
                            .setDetails(value)
                            .setTtl(3600)
                    );
                }

                applyRawDnsEntriesOnMap(desiredEntriesByNameType, configEntries.getConflictResolution(), rawDnsEntries);

            }

            // Azure Custom Domain
            if (configEntries.getAzureCustomDomainDnsEntry() != null) {

                List<RawDnsEntry> rawDnsEntries = new ArrayList<>();
                for (var entry : configEntries.getAzureCustomDomainDnsEntry()) {
                    var webAppOptional = webappFindByNameRaw(entry.getWebappResourceGroupName(), entry.getWebappName());
                    if (webAppOptional.isEmpty()) {
                        context.needsNextStage(entry.getWebappResourceGroupName(), entry.getWebappName());
                        continue;
                    }

                    var webApp = webAppOptional.get();
                    String targetHostname = webApp.defaultHostname();

                    // A or CNAME
                    if (entry.isUseCname()) {
                        rawDnsEntries.add(new RawDnsEntry()
                                .setName(entry.getHostname())
                                .setType("CNAME")
                                .setDetails(targetHostname)
                                .setTtl(300)
                        );
                    } else {
                        try {
                            var records = new Lookup(targetHostname, Type.A).run();
                            var record = Arrays.stream(records).findFirst().orElse(null);
                            if (record == null) {
                                logger.warn("Could not resolve {}. Marking for retry", targetHostname);
                                context.needsNextStage(entry.getWebappResourceGroupName(), entry.getWebappName());
                                continue;
                            }
                            var aRecord = (ARecord) record;
                            rawDnsEntries.add(new RawDnsEntry()
                                    .setName(entry.getHostname())
                                    .setType("A")
                                    .setDetails(aRecord.getAddress().getHostAddress())
                                    .setTtl(300)
                            );
                        } catch (TextParseException e) {
                            logger.warn("Could not resolve {}. Marking for retry", targetHostname, e);
                            context.needsNextStage(entry.getWebappResourceGroupName(), entry.getWebappName());
                        }

                    }

                }

                applyRawDnsEntriesOnMap(desiredEntriesByNameType, configEntries.getConflictResolution(), rawDnsEntries);

            }
        }

        // Update TTL for all with same name/type
        desiredEntriesByNameType.values().forEach(entries -> {
            final var ttl = entries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min);
            entries.forEach(entry -> entry.setTtl(ttl));
        });

        return desiredEntriesByNameType.values().stream()
                .flatMap(Set::stream)
                .filter(it -> dnsIsSubDomain(domainName, it.getName()))
                .sorted().distinct()
                .collect(Collectors.toList());

    }

    private static void applyRawDnsEntriesOnMap(Map<String, Set<RawDnsEntry>> desiredEntriesByNameType, ConflictResolution conflictResolution, List<RawDnsEntry> rawDnsEntries) {
        // Remove any existing ones if OVERWRITE
        if (conflictResolution == ConflictResolution.OVERWRITE) {
            for (RawDnsEntry it : rawDnsEntries) {
                String nameType = it.getName() + "|" + it.getType();
                desiredEntriesByNameType.remove(nameType);
            }
        }

        // Add entries
        for (RawDnsEntry it : rawDnsEntries) {
            String nameType = it.getName() + "|" + it.getType();
            CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                    .add(it);
        }
    }

    protected static boolean dnsIsSubDomain(String baseDomainName, String fullDomainName) {
        if (fullDomainName.length() <= baseDomainName.length()) {
            return baseDomainName.equals(fullDomainName);
        } else return fullDomainName.endsWith("." + baseDomainName);
    }

    protected static String dnsSubDomain(String baseDomainName, String fullDomainName) {
        String subDomain = fullDomainName;

        // Not subdomain
        if (!dnsIsSubDomain(baseDomainName, fullDomainName)) {
            return null;
        }

        if (StringTools.safeEquals(baseDomainName, subDomain)) {
            subDomain = "@";
        } else {
            subDomain = subDomain.substring(0, subDomain.length() - 1 - baseDomainName.length());
        }
        return subDomain;
    }

    public List<AzureApplicationServicePlan> applicationServicePlansFindAll() {

        init();

        return azureResourceManager.appServicePlans().list().stream()
                .map(AzureApplicationServicePlan::from)
                .collect(Collectors.toList());

    }

    private Set<String> applicationServiceCertificateHostnamesWithCertificates(WebApp webApp) {
        return webApp.getHostnameBindings().keySet().stream()
                .filter(hostname -> !hostname.endsWith(".azurewebsites.net"))
                .filter(hostname -> {
                    try {
                        azureResourceManager.appServiceCertificates().getByResourceGroup(webApp.resourceGroupName(), hostname);
                        return true;
                    } catch (ManagementException e) {
                        if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.NOT_FOUND)) {
                            return false;
                        }
                        if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                            return false;
                        }
                        throw e;
                    }
                })
                .collect(Collectors.toSet());
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

    public void applicationServicePlanManage(ManageContext context, ManageConfiguration config, AzureApplicationServicePlan desired) {

        AssertTools.assertNotNull(desired.getName(), "name must be provided");

        if (Strings.isNullOrEmpty(desired.getResourceGroup())) {
            fillResourceGroup(config, desired);
        }
        if (Strings.isNullOrEmpty(desired.getRegionId())) {
            fillRegion(config, desired);
        }

        AssertTools.assertNotNull(desired.getResourceGroup(), "resource group must be provided");
        AssertTools.assertNotNull(desired.getRegionId(), "region must be provided");

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
                    .withRegion(desired.getRegionId())
                    .withExistingResourceGroup(desired.getResourceGroup())
                    .withPricingTier(pricingTier)
                    .withOperatingSystem(operatingSystem)
                    .withCapacity(desired.getCapacity())
                    .withPerSiteScaling(desired.getPerSiteScaling())
                    .create());
            context.addModificationAdd("Azure Application Service Plan", desired.getName());
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

    public void dnsZoneManage(ManageContext context, ManageConfiguration config, AzureDnsZoneManageConfiguration desired) {

        init();

        var desiredResource = desired.getResource();

        AssertTools.assertNotNull(desiredResource, "resource must be provided");
        AssertTools.assertNotNull(desiredResource.getName(), "resource.name must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegionId())) {
            desiredResource.setRegionId("global");
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource.resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegionId(), "resource.region must be provided");

        logger.info("Check {}", desiredResource);
        var currentResource = dnsZoneFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desiredResource);
            var manager = MariaDBManager.authenticate(tokenCredential, profile);
            currentResource = AzureDnsZone.from(azureResourceManager.dnsZones().define(desiredResource.getName())
                    .withExistingResourceGroup(desiredResource.getResourceGroup())
                    .create());
            context.addModificationAdd("Azure DNS Zone", desiredResource.getName());
        } else {
            // Check
            logger.info("Exists: {}", desiredResource);

            var differences = desiredResource.differences(currentResource);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        // Apply entries
        if (desired.getConfig() != null) {
            // Get what is currently present
            var currentEntries = dnsZoneEntryListIgnoreNs(currentResource);
            var desiredEntries = computeDnsEntries(context, desiredResource.getName(), currentEntries, desired.getConfig());

            // Apply
            Set<Tuple2<String, String>> nameTypesToUpdate = new HashSet<>();
            ListsComparator.compareStreams(
                    currentEntries.stream(),
                    desiredEntries.stream(),
                    new ListComparatorHandler<>() {
                        @Override
                        public void both(RawDnsEntry current, RawDnsEntry desired) {
                            // Keep
                        }

                        @Override
                        public void leftOnly(RawDnsEntry current) {
                            logger.info("[{}] Remove: {}", desiredResource.getName(), current);
                            nameTypesToUpdate.add(new Tuple2<>(current.getName(), current.getType()));
                            context.addModificationRemove("Azure DNS Zone", desiredResource.getName(), current.toString());
                        }

                        @Override
                        public void rightOnly(RawDnsEntry desired) {
                            logger.info("[{}] Add: {}", desiredResource.getName(), desired);
                            nameTypesToUpdate.add(new Tuple2<>(desired.getName(), desired.getType()));
                            context.addModificationAdd("Azure DNS Zone", desiredResource.getName(), desired.toString());

                        }
                    }
            );

            if (!nameTypesToUpdate.isEmpty()) {
                logger.info("Get DNS Zone {}", currentResource.getId());
                DnsZone dnsZone = azureResourceManager.dnsZones().getById(currentResource.getId());

                nameTypesToUpdate.forEach(nameType -> {
                    var entryName = nameType.getA();
                    var entryType = nameType.getB();
                    dnsSetEntry(dnsZone, entryName, entryType,
                            desiredEntries.stream()
                                    .filter(it -> StringTools.safeEquals(it.getName(), entryName))
                                    .filter(it -> StringTools.safeEquals(it.getType(), entryType))
                                    .collect(Collectors.toList())
                    );
                });
            }
        }

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

    public List<RawDnsEntry> dnsZoneEntryListIgnoreNs(AzureDnsZone azureDnsZone) {
        var entries = dnsZoneEntryList(azureDnsZone);
        entries.removeIf(it -> StringTools.safeEquals(it.getType(), "NS"));
        return entries;
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
                innerModel.txtRecords().forEach(r -> r.value().forEach(v -> rawDnsEntries.add(new RawDnsEntry() //
                        .setName(trimDot(record.fqdn())) //
                        .setType(record.recordType().name()) //
                        .setDetails(v) //
                        .setTtl(record.timeToLive()) //
                )));
            }
        });

        Collections.sort(rawDnsEntries);
        return rawDnsEntries;
    }

    public void dnsSetEntry(AzureDnsZone azureDnsZone, String entryName, String entryType, List<RawDnsEntry> rawDnsEntries) {

        init();

        logger.info("Get DNS Zone {}", azureDnsZone.getId());
        DnsZone dnsZone = azureResourceManager.dnsZones().getById(azureDnsZone.getId());

        dnsSetEntry(dnsZone, entryName, entryType, rawDnsEntries);

    }

    public void dnsSetEntry(DnsZone dnsZone, String entryName, String entryType, List<RawDnsEntry> rawDnsEntries) {

        AssertTools.assertFalse(
                rawDnsEntries.stream()
                        .anyMatch(it -> !StringTools.safeEquals(it.getName(), entryName) || !StringTools.safeEquals(it.getType(), entryType))
                , "All provided raw entries must be for the same specified name and type");

        logger.info("Set {}/{}", entryName, entryType);
        String baseDomainName = dnsZone.name();
        String subDomain = dnsSubDomain(baseDomainName, entryName);
        if (subDomain == null) {
            logger.error("Skipping {} because it is not a subdomain of {}", entryName, baseDomainName);
            return;
        }
        switch (entryType) {
            case "AAAA":
                dnsZone.update() //
                        .withoutAaaaRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineAaaaRecordSet(subDomain)
                            .withIPv6Address(rawDnsEntries.get(0).getDetails()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withIPv6Address(it.getDetails())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "A":
                dnsZone.update() //
                        .withoutARecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineARecordSet(subDomain) //
                            .withIPv4Address(rawDnsEntries.get(0).getDetails()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withIPv4Address(it.getDetails())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "CNAME":
                dnsZone.update() //
                        .withoutCNameRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    AssertTools.assertTrue(rawDnsEntries.size() == 1, "You can only have 1 CNAME entry");
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineCNameRecordSet(subDomain) //
                            .withAlias(rawDnsEntries.get(0).getDetails()));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "MX":
                dnsZone.update() //
                        .withoutMXRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineMXRecordSet(subDomain) //
                            .withMailExchange(rawDnsEntries.get(0).getDetails(), rawDnsEntries.get(0).getPriority()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withMailExchange(it.getDetails(), it.getPriority())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "NS":
                dnsZone.update() //
                        .withoutNSRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineNSRecordSet(subDomain) //
                            .withNameServer(rawDnsEntries.get(0).getDetails()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withNameServer(it.getDetails())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "SRV":
                dnsZone.update() //
                        .withoutSrvRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineSrvRecordSet(subDomain) //
                            .withRecord(subDomain, rawDnsEntries.get(0).getPort(), rawDnsEntries.get(0).getPriority(), rawDnsEntries.get(0).getWeight()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withRecord(subDomain, it.getPort(), it.getPriority(), it.getWeight())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

            case "TXT":
                dnsZone.update() //
                        .withoutTxtRecordSet(subDomain) //
                        .apply();
                if (!rawDnsEntries.isEmpty()) {
                    var update = new AtomicReference<>(dnsZone.update() //
                            .defineTxtRecordSet(subDomain) //
                            .withText(rawDnsEntries.get(0).getDetails()));
                    forEachAllButFirst(rawDnsEntries, it -> update.set(update.get().withText(it.getDetails())));
                    update.get().withTimeToLive(rawDnsEntries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min)) //
                            .attach() //
                            .apply();
                }
                break;

        }

    }

    public void storageFileShareCreate(String storageAccountId, AzureStorageFileShare storageFileShare) {

        init();

        logger.info("Create Storage File Share {} / {}", storageAccountId, storageFileShare);

        var storageAccount = azureResourceManager.storageAccounts().getById(storageAccountId);
        var key = storageAccount.getKeys().get(0).value();
        storageAccountKeyCache.put(storageAccountKeyCacheKey(storageAccount.resourceGroupName(), storageAccount.name()), key);

        ShareServiceClient shareServiceClient = new ShareServiceClientBuilder().endpoint(storageAccount.endPoints().primary().file())
                .credential(new AzureNamedKeyCredential(storageAccount.name(), key))
                .buildClient();

        shareServiceClient.createShareWithResponse(storageFileShare.getName(), new ShareCreateOptions()
                        .setQuotaInGb(storageFileShare.getQuotaInGB())
                        .setAccessTier(ShareAccessTier.fromString(storageFileShare.getAccessTier()))
                        .setProtocols(new ShareProtocols()
                                .setSmbEnabled(storageFileShare.isSmbEnabled())
                                .setNfsEnabled(storageFileShare.isNfsEnabled())
                        )
                ,
                null, null
        );
    }

    public void storageFileShareDelete(String storageAccountId, String fileShareName) {
        init();

        logger.info("Delete Storage File Share {} / {}", storageAccountId, fileShareName);

        var storageAccount = azureResourceManager.storageAccounts().getById(storageAccountId);
        var key = storageAccount.getKeys().get(0).value();

        ShareServiceClient shareServiceClient = new ShareServiceClientBuilder().endpoint(storageAccount.endPoints().primary().file())
                .credential(new AzureNamedKeyCredential(storageAccount.name(), key))
                .buildClient();

        shareServiceClient.deleteShare(fileShareName);
    }

    public List<AzureStorageFileShare> storageFileShareList(String storageAccountId) {

        init();

        logger.info("List Storage File Share for {}", storageAccountId);
        var storageAccount = azureResourceManager.storageAccounts().getById(storageAccountId);
        var key = storageAccount.getKeys().get(0).value();

        ShareServiceClient shareServiceClient = new ShareServiceClientBuilder().endpoint(storageAccount.endPoints().primary().file())
                .credential(new AzureNamedKeyCredential(storageAccount.name(), key))
                .buildClient();

        return shareServiceClient.listShares().stream()
                .map(AzureStorageFileShare::from)
                .sorted((a, b) -> StringTools.safeComparisonNullFirst(a.getName(), b.getName()))
                .collect(Collectors.toList());

    }

    public void storageFileShareUpload(String resourceGroupName, String storageAccountName, String shareName, String sourceFolder, String targetFolder) {

        init();

        var client = new ShareFileClientBuilder()
                .endpoint(String.format("https://%s.file.core.windows.net", storageAccountName))
                .shareName(shareName)
                .credential(new AzureNamedKeyCredential(storageAccountName, storageAccountKey(resourceGroupName, storageAccountName)))
                .resourcePath(targetFolder)
                .buildDirectoryClient();

        ListsComparator.compareStreams(
                client.listFilesAndDirectories(new ShareListFilesAndDirectoriesOptions().setIncludeTimestamps(true), null, Context.NONE).stream().sorted(Comparator.comparing(ShareFileItem::getName)),
                Arrays.stream(new File(sourceFolder).listFiles()).sorted(Comparator.comparing(File::getName)),
                (a, b) -> a.getName().compareTo(b.getName()),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(ShareFileItem current, File desired) {

                        // Check that it is same type (if not, delete and recreate)
                        if (current.isDirectory() != desired.isDirectory()) {

                            storageFileShareDeleteFileOrDirectory(resourceGroupName, storageAccountName, shareName, targetFolder, client, current);

                            // Recreate
                            storageFileShareCreateFileOrDirectory(resourceGroupName, storageAccountName, shareName, targetFolder, client, desired);

                        } else {

                            // Same type
                            if (current.isDirectory()) {
                                // Walk it
                                var subSourceFolder = desired.getAbsolutePath();
                                var subTargetFolder = targetFolder.isEmpty() ? current.getName() : targetFolder + "/" + current.getName();
                                storageFileShareUpload(resourceGroupName, storageAccountName, shareName, subSourceFolder, subTargetFolder);
                            } else {
                                // Update the file if size or modification date are not the same
                                long currentLastModificationSeconds = current.getProperties().getLastModified().toEpochSecond();
                                long desiredLastModificationSeconds = desired.lastModified() / 1000;
                                if (currentLastModificationSeconds < desiredLastModificationSeconds) {
                                    logger.info("Upload file in {}/{} because the last modification date is locally more recent", targetFolder, desired.getName());
                                    client.createFile(desired.getName(), desired.length())
                                            .uploadFromFile(desired.getAbsolutePath());
                                }
                                if (current.getFileSize() != desired.length()) {
                                    logger.info("Upload file in {}/{} because the size are different", targetFolder, desired.getName());
                                    client.createFile(desired.getName(), desired.length())
                                            .uploadFromFile(desired.getAbsolutePath());
                                }
                            }


                        }

                    }

                    @Override
                    public void leftOnly(ShareFileItem current) {
                        // Delete file or delete folder recursively
                        storageFileShareDeleteFileOrDirectory(resourceGroupName, storageAccountName, shareName, targetFolder, client, current);
                    }

                    @Override
                    public void rightOnly(File desired) {
                        //  Upload file or create directory and walk it
                        storageFileShareCreateFileOrDirectory(resourceGroupName, storageAccountName, shareName, targetFolder, client, desired);
                    }
                }
        );

    }

    private void storageFileShareCreateFileOrDirectory(String resourceGroupName, String storageAccountName, String shareName, String targetFolder, ShareDirectoryClient client, File fileOrFolderToCreate) {
        if (fileOrFolderToCreate.isDirectory()) {
            logger.info("Create folder {}/{}", targetFolder, fileOrFolderToCreate.getName());
            client.createSubdirectory(fileOrFolderToCreate.getName());
            var subSourceFolder = fileOrFolderToCreate.getAbsolutePath();
            var subTargetFolder = targetFolder.isEmpty() ? fileOrFolderToCreate.getName() : targetFolder + "/" + fileOrFolderToCreate.getName();
            storageFileShareUpload(resourceGroupName, storageAccountName, shareName, subSourceFolder, subTargetFolder);
        } else {
            logger.info("Upload file in {}/{}", targetFolder, fileOrFolderToCreate.getName());
            client.createFile(fileOrFolderToCreate.getName(), fileOrFolderToCreate.length())
                    .uploadFromFile(fileOrFolderToCreate.getAbsolutePath());
        }
    }

    private void storageFileShareDeleteFileOrDirectory(String resourceGroupName, String storageAccountName, String shareName, String targetFolder, ShareDirectoryClient client, ShareFileItem toDelete) {
        if (toDelete.isDirectory()) {

            var subTargetFolder = targetFolder.isEmpty() ? toDelete.getName() : targetFolder + "/" + toDelete.getName();
            storageFileShareDeleteAllInFolder(resourceGroupName, storageAccountName, shareName, subTargetFolder);
            logger.info("Delete folder {}", subTargetFolder);
            client.deleteSubdirectory(toDelete.getName());

        } else {

            logger.info("Delete file {}/{}", targetFolder, toDelete.getName());
            client.deleteFile(toDelete.getName());

        }
    }

    public void storageFileShareDeleteAllInFolder(String resourceGroupName, String storageAccountName, String shareName, String folderToDelete) {

        init();

        var client = new ShareFileClientBuilder()
                .endpoint(String.format("https://%s.file.core.windows.net", storageAccountName))
                .shareName(shareName)
                .credential(new AzureNamedKeyCredential(storageAccountName, storageAccountKey(resourceGroupName, storageAccountName)))
                .resourcePath(folderToDelete)
                .buildDirectoryClient();

        logger.info("Delete all in folder {}", folderToDelete);
        client.listFilesAndDirectories().forEach(shareFileItem -> {
            storageFileShareDeleteFileOrDirectory(resourceGroupName, storageAccountName, shareName, folderToDelete, client, shareFileItem);
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
                    String defaultSubscriptionId = subscription.getId();
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
            var forceAzCliAuth = SystemTools.getPropertyOrEnvironment("FORCE_AZ_CLI_AUTH", "false").toLowerCase().equals("true");
            TokenCredential wrappedTokenCredential;
            if (forceAzCliAuth) {
                wrappedTokenCredential = new AzureCliCredentialBuilder()
                        .build();
            } else {
                wrappedTokenCredential = new DefaultAzureCredentialBuilder() //
                        .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()) //
                        .build();
            }

            tokenCredential = new TokenCredential() {
                @Override
                public Mono<AccessToken> getToken(TokenRequestContext request) {
                    logger.info("Get token for Scopes {}, tenant id {}, claims {}", request.getScopes(), request.getTenantId(), request.getClaims());
                    return wrappedTokenCredential.getToken(request);
                }
            };
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

    public void keyVaultManage(ManageContext context, ManageConfiguration config, AzureKeyVault desiredResource) {

        AssertTools.assertNotNull(desiredResource.getName(), "name must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegionId())) {
            fillRegion(config, desiredResource);
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegionId(), "region must be provided");

        logger.info("Check {}", desiredResource);
        var currentResource = keyVaultFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desiredResource);
            currentResource = keyVaultCreate(desiredResource.getResourceGroup(), Optional.of(desiredResource.getRegionId()), desiredResource.getName());
            context.addModificationAdd("Azure Key Vault", desiredResource.getName());
        } else {
            // Check
            logger.info("Exists: {}", desiredResource);

            var differences = desiredResource.differences(currentResource);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

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

    public void mariadbManage(ManageContext context, ManageConfiguration config, AzureMariadbManageConfiguration desired) {

        var desiredResource = desired.getResource();

        AssertTools.assertNotNull(desiredResource, "resource must be provided");
        AssertTools.assertNotNull(desiredResource.getName(), "resource.name must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegionId())) {
            fillRegion(config, desiredResource);
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource.resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegionId(), "resource.region must be provided");

        logger.info("Check {}", desiredResource);
        var currentResource = mariadbFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desiredResource);
            var manager = MariaDBManager.authenticate(tokenCredential, profile);
            currentResource = AzureMariadb.from(manager.servers().define(desiredResource.getName())
                    .withRegion(desiredResource.getRegionId())
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
            context.addModificationAdd("Azure MariaDB", desiredResource.getName());
        } else {
            // Check
            logger.info("Exists: {}", desiredResource);

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

    public void resourceGroupManage(ManageContext context, AzureResourceGroup desired) {
        AssertTools.assertNotNull(desired.getName(), "name must be provided");
        AssertTools.assertNotNull(desired.getRegionId(), "region must be provided");

        logger.info("Check: {}", desired);
        var current = resourceGroupFindByName(desired.getName()).orElse(null);
        if (current == null) {
            // create
            logger.info("Create: {}", desired);
            current = AzureResourceGroup.from(azureResourceManager.resourceGroups().define(desired.getName())
                    .withRegion(desired.getRegionId())
                    .create());
            context.addModificationAdd("Azure Resource Group", desired.getName());
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

    }

    public String storageAccountKey(String resourceGroupName, String storageAccountName) {

        init();

        try {
            return storageAccountKeyCache.get(storageAccountKeyCacheKey(resourceGroupName, storageAccountName), () -> {
                var storageAccount = azureResourceManager.storageAccounts().getByResourceGroup(resourceGroupName, storageAccountName);
                return storageAccount.getKeys().get(0).value();
            });
        } catch (ExecutionException e) {
            throw new ManageUnrecoverableException("Problem getting storage account key", e);
        }

    }

    public Optional<AzureStorageAccount> storageAccountFindByName(String resourceGroupName, String storageAccountName) {

        init();

        logger.info("Get Storage Account {} / {}", resourceGroupName, storageAccountName);
        try {
            var storageAccount = azureResourceManager.storageAccounts().getByResourceGroup(resourceGroupName, storageAccountName);
            var azureStorageAccount = AzureStorageAccount.from(storageAccount);
            azureStorageAccount.setAzureFileShares(
                    storageFileShareList(azureStorageAccount.getId())
            );
            return Optional.of(azureStorageAccount);
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
        }

    }

    public List<AzureStorageAccount> storageAccountList() {

        init();

        logger.info("List Storage Accounts");
        var it = azureResourceManager.storageAccounts().list();
        return it.stream()
                .map(AzureStorageAccount::from)
                .sorted((a, b) -> StringTools.safeComparisonNullFirst(a.getName(), b.getName()))
                .peek(storageAccount -> storageAccount.setAzureFileShares(
                        storageFileShareList(storageAccount.getId())
                ))
                .collect(Collectors.toList());
    }

    public void storageAccountManage(ManageContext context, ManageConfiguration config, AzureStorageAccount desiredResource) {

        AssertTools.assertNotNull(desiredResource.getName(), "name must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegionId())) {
            fillRegion(config, desiredResource);
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegionId(), "region must be provided");

        StorageAccountSkuType sku;
        if (desiredResource.getSkuName() == null) {
            sku = StorageAccountSkuType.STANDARD_LRS;
        } else {
            sku = StorageAccountSkuType.fromSkuName(SkuName.fromString(desiredResource.getSkuName()));
        }

        logger.info("Check {}", desiredResource);
        var currentResource = storageAccountFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desiredResource);
            currentResource = AzureStorageAccount.from(azureResourceManager.storageAccounts().define(desiredResource.getName())
                    .withRegion(desiredResource.getRegionId())
                    .withExistingResourceGroup(desiredResource.getResourceGroup())
                    .withSku(sku)
                    .withLargeFileShares(desiredResource.getLargeFileShares() == null ? false : desiredResource.getLargeFileShares())
                    .create());
            context.addModificationAdd("Azure Storage Account", desiredResource.getName());
        } else {
            // Check
            logger.info("Exists: {}", desiredResource);

            var differences = desiredResource.differences(currentResource);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }
        }

        // File shares
        final var fCurrentResource = currentResource;
        logger.info("Managing file shares on: {}", currentResource.getId());
        ListsComparator.compareStreams(
                currentResource.getAzureFileShares().stream().sorted(),
                desiredResource.getAzureFileShares().stream().sorted(),
                (a, b) -> a.getName().compareTo(b.getName()),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(AzureStorageFileShare currentStorageFileShare, AzureStorageFileShare desiredStorageFileShare) {
                        // Compare
                        logger.info("Exists: {}", desiredStorageFileShare);

                        var differences = desiredStorageFileShare.differences(currentStorageFileShare);
                        if (!differences.isEmpty()) {
                            for (String difference : differences) {
                                logger.error(difference);
                            }
                            throw new ManageUnrecoverableException();
                        }
                    }

                    @Override
                    public void leftOnly(AzureStorageFileShare currentStorageFileShare) {
                        // Remove
                        storageFileShareDelete(fCurrentResource.getId(), currentStorageFileShare.getName());
                        context.addModificationRemove("Azure Storage Account", desiredResource.getName() + "/" + currentStorageFileShare.getName());
                    }

                    @Override
                    public void rightOnly(AzureStorageFileShare desiredStorageFileShare) {
                        // Create
                        storageFileShareCreate(fCurrentResource.getId(), desiredStorageFileShare);
                        context.addModificationAdd("Azure Storage Account", desiredResource.getName() + "/" + desiredStorageFileShare.getName());
                    }
                }
        );

    }

    public List<AzureWebApp> webappList() {

        init();

        return azureResourceManager.webApps().list().stream()
                .map(it -> {
                            var webApp = webappFindByIdRaw(it.id()).get();
                            return AzureWebApp.from(
                                    webApp,
                                    azureCustomClient.websiteConfig(it.id()).getSingleValue(),
                                    applicationServiceCertificateHostnamesWithCertificates(webApp)
                            );
                        }
                )
                .collect(Collectors.toList());

    }

    public void webappManage(ManageContext context, ManageConfiguration config, AzureWebAppManageConfiguration desired) {

        init();

        var desiredResource = desired.getResource();

        AssertTools.assertNotNull(desiredResource, "resource must be provided");
        AssertTools.assertNotNull(desiredResource.getName(), "resource.name must be provided");
        AssertTools.assertNotNull(desiredResource.getAppServicePlanId(), "resource.appServicePlanId must be provided");

        if (Strings.isNullOrEmpty(desiredResource.getResourceGroup())) {
            fillResourceGroup(config, desiredResource);
        }
        if (Strings.isNullOrEmpty(desiredResource.getRegionId())) {
            fillRegion(config, desiredResource);
        }

        AssertTools.assertNotNull(desiredResource.getResourceGroup(), "resource.resource group must be provided");
        AssertTools.assertNotNull(desiredResource.getRegionId(), "resource.region must be provided");

        // Create or get website
        logger.info("Check {}", desiredResource);
        var currentResource = webappFindByName(desiredResource.getResourceGroup(), desiredResource.getName()).orElse(null);
        if (currentResource == null) {
            // create
            logger.info("Create: {}", desiredResource);
            AppServicePlan appServicePlan = azureResourceManager.appServicePlans().getById(desiredResource.getAppServicePlanId());
            var webapp = azureResourceManager.webApps().define(desiredResource.getName())
                    .withExistingLinuxPlan(appServicePlan)
                    .withExistingResourceGroup(desiredResource.getResourceGroup())
                    .withPublicDockerHubImage(desiredResource.getPublicDockerHubImage())
                    .withAppSettings(desiredResource.getAppSettings())
                    .withHttpsOnly(desiredResource.getHttpsOnly())
                    .withWebAppAlwaysOn(desiredResource.getAlwaysOn())
                    .withWebSocketsEnabled(desiredResource.getWebSocketsEnabled())
                    .create();
            currentResource = AzureWebApp.from(webapp,
                    azureCustomClient.websiteConfig(webapp.id()).getSingleValue(),
                    applicationServiceCertificateHostnamesWithCertificates(webapp)
            );
            context.addModificationAdd("Azure Web Application", desiredResource.getName());
        } else {
            // Check
            logger.info("Exists: {}", desiredResource);

            List<Function<WebApp, List<Modification>>> updateActions = new ArrayList<>();
            var differences = desiredResource.differences(currentResource, updateActions);
            if (!differences.isEmpty()) {
                for (String difference : differences) {
                    logger.error(difference);
                }
                throw new ManageUnrecoverableException();
            }

            // Update what can be updated
            if (!updateActions.isEmpty()) {
                logger.info("Azure Web Application - Update some properties of {}", desiredResource.getName());
                var resourceId = currentResource.getId();
                updateActions.forEach(it -> it.apply(azureResourceManager.webApps().getById(resourceId)).forEach(context::addModification));
            }
        }

        // Update shares
        desiredResource.setId(currentResource.getId());
        var hasChanges = new AtomicBoolean(false);
        ListsComparator.compareStreams(
                currentResource.getMountStorages().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)),
                desiredResource.getMountStorages().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)),
                (left, right) -> left.getKey().compareTo(right.getKey()),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(Map.Entry<String, AzureWebAppMountStorage> current, Map.Entry<String, AzureWebAppMountStorage> desired) {
                        // Check if different
                        if (!current.equals(desired)) {
                            logger.info("[{}] Update Azure Web Application & Storage: {} -> {}", desiredResource.getName(), current, desired);
                            hasChanges.set(true);
                            context.addModificationUpdate("Azure Web Application & Storage", desiredResource.getName() + "/" + current.getKey(), "", current.getValue().toString(), desired.getValue().toString());
                        }
                    }

                    @Override
                    public void leftOnly(Map.Entry<String, AzureWebAppMountStorage> current) {
                        logger.info("[{}] Remove Azure Web Application & Storage: {}", desiredResource.getName(), current);
                        hasChanges.set(true);
                        context.addModificationRemove("Azure Web Application & Storage", desiredResource.getName() + "/" + current.getKey(), current.getValue().toString());
                    }

                    @Override
                    public void rightOnly(Map.Entry<String, AzureWebAppMountStorage> desired) {
                        logger.info("[{}] Add Azure Web Application & Storage: {}", desiredResource.getName(), desired);
                        hasChanges.set(true);
                        context.addModificationAdd("Azure Web Application & Storage", desiredResource.getName() + "/" + desired.getKey(), desired.getValue().toString());
                    }
                }
        );
        if (hasChanges.get()) {
            logger.info("[{}] Update mounts", desiredResource.getName());
            azureCustomClient.websiteMountStorageUpdate(desiredResource.getId(), desiredResource.getResourceGroup(), desiredResource.getMountStorages());
        }

        // Update custom domains
        hasChanges.set(false);
        ListsComparator.compareStreams(
                currentResource.getCustomHostnames().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)),
                desiredResource.getCustomHostnames().entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)),
                (left, right) -> left.getKey().compareTo(right.getKey()),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(Map.Entry<String, AzureWebAppCustomHostname> current, Map.Entry<String, AzureWebAppCustomHostname> desired) {
                        // Keep
                    }

                    @Override
                    public void leftOnly(Map.Entry<String, AzureWebAppCustomHostname> current) {
                        logger.info("[{}] Remove Azure Web Application & Custom Domain: {}", desiredResource.getName(), current);
                        try {
                            var webApp = azureResourceManager.webApps().getById(desiredResource.getId());
                            webApp.update().withoutHostnameBinding(current.getKey())
                                    .apply();
                            context.addModificationRemove("Azure Web Application & Custom Domain", desiredResource.getName() + "/" + current.getKey());
                        } catch (Exception e) {
                            logger.warn("Problem removing the custom domain {}. Error: [{}]. Retry later", current.getKey(), e.getMessage());
                            context.needsNextStage(desiredResource.getName(), current.getKey());
                        }

                    }

                    @Override
                    public void rightOnly(Map.Entry<String, AzureWebAppCustomHostname> desired) {
                        logger.info("[{}] Add Azure Web Application & Custom Domain: {}", desiredResource.getName(), desired);
                        String domainName = desired.getValue().getDomainName();
                        try {
                            var webApp = azureResourceManager.webApps().getById(desiredResource.getId());
                            webApp.update().defineHostnameBinding()
                                    .withThirdPartyDomain(domainName)
                                    .withSubDomain(desired.getKey())
                                    .withDnsRecordType(StringTools.safeEquals(domainName, desired.getKey()) ? CustomHostnameDnsRecordType.A : CustomHostnameDnsRecordType.CNAME)
                                    .attach()
                                    .apply()
                            ;
                            context.addModificationAdd("Azure Web Application & Custom Domain", desiredResource.getName() + "/" + desired.getKey());
                        } catch (Exception e) {
                            logger.warn("Problem adding the custom domain {}. Error: [{}]. Retry later", desired.getKey(), e.getMessage());
                            context.needsNextStage(desiredResource.getName(), desired.getKey());
                        }

                    }
                }
        );

        // Create App Service Managed Certificate
        var currentCertificates = applicationServiceCertificateHostnamesWithCertificates(azureResourceManager.webApps().getById(desiredResource.getId()));
        var desiredCustomHostnameCerts = desiredResource.getCustomHostnames().entrySet().stream()
                .filter(it -> it.getValue().isCreateCertificate())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        ListsComparator.compareStreams(
                currentCertificates.stream().sorted(),
                desiredCustomHostnameCerts.stream().sorted(),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(String current, String desired) {
                        // Keep
                    }

                    @Override
                    public void leftOnly(String current) {
                        logger.info("[{}] Remove Azure Web Application & Certificate: {}", desiredResource.getName(), current);
                        try {
                            azureResourceManager.appServiceCertificates().deleteByResourceGroup(desiredResource.getResourceGroup(), current);
                            context.addModificationRemove("Azure Web Application & Certificate", desiredResource.getName() + "/" + current);
                        } catch (Exception e) {
                            logger.warn("Problem removing the certificate for domain {}. Error: [{}]. Retry later", current, e.getMessage(), e);
                            context.needsNextStage(desiredResource.getName(), current);
                        }
                    }

                    @Override
                    public void rightOnly(String desired) {
                        logger.info("[{}] Add Azure Web Application & Certificate: {}", desiredResource.getName(), desired);
                        try {
                            azureCustomClient.applicationServiceCertificateCreate(desiredResource, desiredResource.getCustomHostnames().get(desired).getDomainName(), desired);
                            context.addModificationAdd("Azure Web Application & Certificate", desiredResource.getName() + "/" + desired);
                        } catch (Exception e) {
                            logger.warn("Problem adding the certificate for domain {}. Error: [{}]. Retry later", desired, e.getMessage(), e);
                            context.needsNextStage(desiredResource.getName(), desired);
                        }
                    }
                }
        );

        // Add binding
        var currentBindedCustomNames = azureResourceManager.webApps().getById(desiredResource.getId()).getHostnameBindings().entrySet().stream()
                .filter(it -> !it.getKey().endsWith(".azurewebsites.net"))
                .filter(it -> it.getValue().innerModel().sslState() != null)
                .filter(it -> it.getValue().innerModel().sslState() != SslState.DISABLED)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        ListsComparator.compareStreams(
                currentBindedCustomNames.stream().sorted(),
                desiredCustomHostnameCerts.stream().sorted(),
                new ListComparatorHandler<>() {
                    @Override
                    public void both(String current, String desired) {
                        // Keep
                    }

                    @Override
                    public void leftOnly(String current) {
                        logger.info("[{}] Remove Azure Web Application & Certificate Binding: {}", desiredResource.getName(), current);
                        try {
                            var webApp = azureResourceManager.webApps().getById(desiredResource.getId());
                            webApp.update().withoutSslBinding(current).apply();
                            context.addModificationRemove("Azure Web Application & Certificate Binding", desiredResource.getName() + "/" + current);
                        } catch (Exception e) {
                            logger.warn("Problem removing the certificate binding for domain {}. Error: [{}]. Retry later", current, e.getMessage(), e);
                            context.needsNextStage(desiredResource.getName(), current);
                        }
                    }

                    @Override
                    public void rightOnly(String desired) {
                        logger.info("[{}] Add Azure Web Application & Certificate Binding: {}", desiredResource.getName(), desired);
                        try {
                            var webApp = azureResourceManager.webApps().getById(desiredResource.getId());
                            webApp.update().defineSslBinding()
                                    .forHostname(desired)
                                    .withExistingCertificate(desired)
                                    .withSniBasedSsl()
                                    .attach()
                                    .apply();
                            context.addModificationAdd("Azure Web Application & Certificate Binding", desiredResource.getName() + "/" + desired);
                        } catch (Exception e) {
                            logger.warn("Problem adding the certificate binding for domain {}. Error: [{}]. Retry later", desired, e.getMessage(), e);
                            context.needsNextStage(desiredResource.getName(), desired);
                        }
                    }
                }
        );

    }

    public Optional<AzureWebApp> webappFindById(String azureWebappId) {

        init();

        var resourceOptional = webappFindByIdRaw(azureWebappId);
        if (resourceOptional.isEmpty()) {
            return Optional.empty();
        }
        var resource = resourceOptional.get();
        return Optional.of(AzureWebApp.from(
                resource,
                azureCustomClient.websiteConfig(azureWebappId).getSingleValue(),
                applicationServiceCertificateHostnamesWithCertificates(resource)
        ));
    }

    public Optional<WebApp> webappFindByIdRaw(String azureWebappId) {

        init();

        try {
            return Optional.of(azureResourceManager.webApps().getById(azureWebappId));
        } catch (ResourceNotFoundException e) {
            return Optional.empty();
        }
    }

    public Optional<AzureWebApp> webappFindByName(String resourceGroupName, String name) {

        init();

        var resourceOptional = webappFindByNameRaw(resourceGroupName, name);
        if (resourceOptional.isEmpty()) {
            return Optional.empty();
        }
        var resource = resourceOptional.get();
        return Optional.of(AzureWebApp.from(
                resource,
                azureCustomClient.websiteConfig(resource.id()).getSingleValue(),
                applicationServiceCertificateHostnamesWithCertificates(resource)
        ));
    }

    public Optional<WebApp> webappFindByNameRaw(String resourceGroupName, String name) {

        init();

        try {
            return Optional.of(azureResourceManager.webApps().getByResourceGroup(resourceGroupName, name));
        } catch (ManagementException e) {
            if (StringTools.safeEquals(e.getValue().getCode(), AzureConstants.RESOURCE_NOT_FOUND)) {
                return Optional.empty();
            }
            throw e;
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
            hasRegion.setRegionId(config.getAzureResourceGroups().get(0).getRegionId());
        }
    }

    private void fillResourceGroup(ManageConfiguration config, HasResourceGroup hasResourceGroup) {
        if (config.getAzureResourceGroups().size() == 1) {
            hasResourceGroup.setResourceGroup(config.getAzureResourceGroups().get(0).getName());
        }
    }

    private <T> void forEachAllButFirst(List<T> items, Consumer<T> action) {
        var it = items.iterator();

        // Skip first
        if (it.hasNext()) {
            it.next();
        }

        // All the rest
        while (it.hasNext()) {
            var next = it.next();
            action.accept(next);
        }
    }

    public String getTokenManagement() {
        init();

        try {
            return tokenCache.get("https://management.core.windows.net//.default");
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
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
