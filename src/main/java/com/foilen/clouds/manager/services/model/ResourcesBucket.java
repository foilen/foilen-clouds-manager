/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.CollectionsTools;

public class ResourcesBucket extends AbstractBasics {

    private SortedMap<String, DomainConfiguration> configurationByDomain = new TreeMap<>();
    private SortedMap<String, List<SecretStore>> secretStoresByGroup = new TreeMap<>();

    public void addDomain(String domainName, DnsZone dnsZone) {
        DomainConfiguration configuration = CollectionsTools.getOrCreateEmpty(configurationByDomain, domainName, DomainConfiguration.class);
        configuration.setDomainName(domainName);

        configuration.getDnsZones().add(dnsZone);
    }

    public void addDomain(String domainName, WebApp webApp, boolean ssl) {
        DomainConfiguration configuration = CollectionsTools.getOrCreateEmpty(configurationByDomain, domainName, DomainConfiguration.class);
        configuration.setDomainName(domainName);

        if (ssl) {
            configuration.getHttpsWebApp().add(webApp);
        } else {
            configuration.getHttpWebApp().add(webApp);
        }
    }

    public void addSecretStore(String groupName, SecretStore secretStore) {
        CollectionsTools.getOrCreateEmptyArrayList(secretStoresByGroup, groupName, SecretStore.class) //
                .add(secretStore);
    }

    public SortedMap<String, DomainConfiguration> getConfigurationByDomain() {
        return configurationByDomain;
    }

    public SortedMap<String, List<SecretStore>> getSecretStoresByGroup() {
        return secretStoresByGroup;
    }

    public void setConfigurationByDomain(SortedMap<String, DomainConfiguration> configurationByDomain) {
        this.configurationByDomain = configurationByDomain;
    }

    public void setSecretStoresByGroup(SortedMap<String, List<SecretStore>> secretStoresByGroup) {
        this.secretStoresByGroup = secretStoresByGroup;
    }

}
