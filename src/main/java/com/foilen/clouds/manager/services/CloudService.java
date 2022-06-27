/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.*;
import com.foilen.smalltools.crypt.bouncycastle.cert.RSACertificate;
import com.foilen.smalltools.tools.AbstractBasics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class CloudService extends AbstractBasics {

    @Autowired
    private CloudAzureService cloudAzureService;

    private void addGroupToListIfNotPresent(List<String> groups, Object item) {
        if (item != null && item instanceof HasResourceGroup) {
            String resourceGroup = ((HasResourceGroup) item).getResourceGroup();
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
                return cloudAzureService.dnsZoneEntryList((AzureDnsZone) dnsZone);
        }

        throw new CliException("Unknown provider");
    }

    public void dnsSetEntry(DnsZone dnsZone, RawDnsEntry rawDnsEntry) {

        switch (dnsZone.getProvider()) {
            case AZURE:
                cloudAzureService.dnsSetEntry((AzureDnsZone) dnsZone, rawDnsEntry.getName(), rawDnsEntry.getType(), Collections.singletonList(rawDnsEntry));
                return;
        }

        throw new CliException("Unknown provider");

    }

    public CloudAzureService getCloudAzureService() {
        return cloudAzureService;
    }

    public void setCloudAzureService(CloudAzureService cloudAzureService) {
        this.cloudAzureService = cloudAzureService;
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

}
