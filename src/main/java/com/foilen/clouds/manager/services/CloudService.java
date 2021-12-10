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

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.AzureDnsZone;
import com.foilen.clouds.manager.services.model.AzureKeyVault;
import com.foilen.clouds.manager.services.model.AzureResourceGroup;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.DnsZone;
import com.foilen.clouds.manager.services.model.DomainConfiguration;
import com.foilen.clouds.manager.services.model.ResourcesBucket;
import com.foilen.clouds.manager.services.model.SecretStore;
import com.foilen.clouds.manager.services.model.WebApp;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class CloudService extends AbstractBasics {

    @Autowired
    private CloudAzureService cloudAzureService;

    private void addGroupToListIfNotPresent(List<String> groups, Object item) {
        if (item != null && item instanceof AzureResourceGroup) {
            String resourceGroup = ((AzureResourceGroup) item).getResourceGroup();
            if (resourceGroup != null) {
                if (!groups.contains(resourceGroup)) {
                    groups.add(resourceGroup);
                }
            }
        }
    }

    private String azureKeyFullName(String namespace, String name) {
        return namespace + "--" + name;
    }

    public List<RawDnsEntry> dnsListEntries(DnsZone dnsZone, String hostname) {

        switch (dnsZone.getProvider()) {
        case AZURE:
            return cloudAzureService.dnsListEntries((AzureDnsZone) dnsZone);
        }

        throw new CliException("Unknown provider");
    }

    public void dnsSetEntry(DnsZone dnsZone, RawDnsEntry rawDnsEntry) {

        switch (dnsZone.getProvider()) {
        case AZURE:
            cloudAzureService.dnsSetEntry((AzureDnsZone) dnsZone, rawDnsEntry);
            return;
        }

        throw new CliException("Unknown provider");

    }

    public CloudAzureService getCloudAzureService() {
        return cloudAzureService;
    }

    public void pushCertificate(String hostname, WebApp httpsWebApp, RSACertificate caRsaCertificate, RSACertificate rsaCertificate, String pfxPassword) {

        switch (httpsWebApp.getProvider()) {
        case AZURE:
            cloudAzureService.webAppServicePushCertificate(hostname, (AzureWebApp) httpsWebApp, caRsaCertificate, rsaCertificate, pfxPassword);
            return;
        }

        throw new CliException("Unknown provider");
    }

    public String secretGetAsText(SecretStore secretStore, String namespace, String name) {

        switch (secretStore.getProvider()) {
        case AZURE:
            return cloudAzureService.keyVaultSecretGetAsText((AzureKeyVault) secretStore, azureKeyFullName(namespace, name));
        }

        throw new CliException("Unknown provider");
    }

    public void secretSetAsTextOrFail(SecretStore secretStore, String namespace, String name, String value) {

        switch (secretStore.getProvider()) {
        case AZURE:
            cloudAzureService.keyVaultSecretSetAsTextOrFail((AzureKeyVault) secretStore, azureKeyFullName(namespace, name), value);
            return;
        }

        throw new CliException("Unknown provider");

    }

    public SecretStore secretStoreFindOrFail(ResourcesBucket resourcesBucket, DomainConfiguration configuration, String prefixName) {

        logger.info("Find an existing secret store for {} with prefix name {}", configuration.getDomainName(), prefixName);

        List<String> usedResourceGroups = new ArrayList<>();
        configuration.getHttpsWebApp().forEach(item -> addGroupToListIfNotPresent(usedResourceGroups, item));
        configuration.getHttpWebApp().forEach(item -> addGroupToListIfNotPresent(usedResourceGroups, item));
        configuration.getDnsZones().forEach(item -> addGroupToListIfNotPresent(usedResourceGroups, item));
        logger.info("Preferred groups order {}", usedResourceGroups);

        SecretStore secretStore = null;
        for (String preferredGroup : usedResourceGroups) {
            List<SecretStore> secretStores = resourcesBucket.getSecretStoresByGroup().get(preferredGroup);
            if (secretStores != null && !secretStores.isEmpty()) {
                for (SecretStore nextSecretStore : secretStores) {
                    if (nextSecretStore.getName().startsWith(prefixName)) {
                        secretStore = nextSecretStore;
                        break;
                    }
                }

            }
        }

        if (secretStore == null) {
            logger.info("Didn't find any secret store prefixed with {} in these resource groups: {}", prefixName, usedResourceGroups);

            throw new CliException("No available secret store");
        }

        logger.info("Will use secret store {}", secretStore.getName(), secretStore.getId());
        return secretStore;
    }

    public void setCloudAzureService(CloudAzureService cloudAzureService) {
        this.cloudAzureService = cloudAzureService;
    }

}
