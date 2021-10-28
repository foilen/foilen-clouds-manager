/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.rest.PagedIterable;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.appservice.models.SslState;
import com.azure.resourcemanager.appservice.models.WebAppBasic;
import com.azure.resourcemanager.dns.fluent.models.RecordSetInner;
import com.azure.resourcemanager.dns.models.CnameRecord;
import com.azure.resourcemanager.dns.models.DnsZone;
import com.azure.resourcemanager.keyvault.models.Vault;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.AzureDnsZone;
import com.foilen.clouds.manager.services.model.AzureKeyVault;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.ResourcesBucket;
import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.smalltools.JavaEnvironmentValues;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.FileTools;
import com.foilen.smalltools.tools.JsonTools;

@Component
public class CloudAzureService extends AbstractBasics {

    @Autowired
    private ResourcesBucketService resourcesBucketService;

    private AzureProfile profile;
    private TokenCredential tokenCredential;
    private AzureResourceManager azureResourceManager;

    public void discoverAll() {

        init();

        ResourcesBucket resourcesBucket = new ResourcesBucket();

        discoverAllDnsZones(resourcesBucket);
        discoverAllWebAppServices(resourcesBucket);
        discoverAllKeyVaults(resourcesBucket);

        resourcesBucketService.setAzureResourcesBucket(resourcesBucket);

    }

    private void discoverAllDnsZones(ResourcesBucket resourcesBucket) {

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

    private void discoverAllKeyVaults(ResourcesBucket resourcesBucket) {

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

    private void discoverAllWebAppServices(ResourcesBucket resourcesBucket) {

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

    private void init() {

        logger.info("Prepare Azure client");
        if (profile == null) {

            String azureProfileFile = JavaEnvironmentValues.getHomeDirectory() + "/.azure/azureProfile.json";
            if (FileTools.exists(azureProfileFile)) {
                AzProfileDetails azProfileDetails = JsonTools.readFromFile(azureProfileFile, AzProfileDetails.class);
                if (!azProfileDetails.getSubscriptions().isEmpty()) {
                    String tenantId = azProfileDetails.getSubscriptions().get(0).getTenantId();
                    logger.info("Using tenant id {}", tenantId);
                    profile = new AzureProfile(tenantId, null, AzureEnvironment.AZURE);
                }
            }
        }
        if (profile == null) {
            profile = new AzureProfile(AzureEnvironment.AZURE);
        }

        if (tokenCredential == null) {
            tokenCredential = new DefaultAzureCredentialBuilder() //
                    .authorityHost(profile.getEnvironment().getActiveDirectoryEndpoint()) //
                    .build();
        }

        if (azureResourceManager == null) {
            azureResourceManager = AzureResourceManager.configure() //
                    .withLogLevel(HttpLogDetailLevel.BASIC) //
                    .authenticate(tokenCredential, profile) //
                    .withDefaultSubscription();
        }

    }

    public AzureKeyVault keyVaultCreate(String keyVaultName, String regionName, String resourceGroup) {

        init();

        Vault vault = azureResourceManager.vaults().define(keyVaultName) //
                .withRegion(regionName) //
                .withExistingResourceGroup(resourceGroup) //
                .withEmptyAccessPolicy() //
                .create();

        AzureKeyVault azureKeyVault = AzureKeyVault.from(vault);

        resourcesBucketService.updateAzureResourcesBucket(resourcesBucket -> {
            resourcesBucket.addSecretStore(resourceGroup, azureKeyVault);
        });

        return azureKeyVault;
    }

    public List<RawDnsEntry> listDnsEntries(AzureDnsZone azureDnsZone) {

        init();

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
                        .setPriority(r.preference())
                        // .setWeight(record.)
                        // .setPort(record.po)
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

    private String trimDot(String name) {
        while (name.endsWith(".")) {
            name = name.substring(0, name.length() - 1);
        }
        return name;
    }
}
