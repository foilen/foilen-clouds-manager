/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.services.model.InMemorySecretStore;
import com.foilen.smalltools.tools.AbstractBasics;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class CloudInMemoryService extends AbstractBasics {

    private ConcurrentMap<String, ConcurrentMap<String, String>> inMemorySecretStore = new ConcurrentHashMap<>();

    public ConcurrentMap<String, String> keyVaultSecretGetAll(InMemorySecretStore secretStore) {
        return getValueByKey(secretStore);
    }

    public String keyVaultSecretGetAsText(InMemorySecretStore secretStore, String key) {
        var valueByKey = getValueByKey(secretStore);
        return valueByKey.get(key);
    }

    public void keyVaultSecretSetAsTextOrFail(InMemorySecretStore secretStore, String key, String value) {
        var valueByKey = getValueByKey(secretStore);
        valueByKey.put(key, value);
    }

    private ConcurrentMap<String, String> getValueByKey(InMemorySecretStore secretStore) {
        return inMemorySecretStore.computeIfAbsent(secretStore.getName(), k -> new ConcurrentHashMap<>());
    }
    
}
