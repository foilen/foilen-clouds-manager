/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.util.function.Consumer;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.clouds.manager.services.model.DomainConfiguration;
import com.foilen.clouds.manager.services.model.ResourcesBucket;
import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class ResourcesBucketService extends AbstractBasics {

    @Autowired
    private ResourcesBucketDao azureResourcesBucketDao;

    private ResourcesBucket azureResourcesBucket = new ResourcesBucket();

    private ResourcesBucket allResourcesBucket = new ResourcesBucket();

    public ResourcesBucket getAllResourcesBucket() {
        return allResourcesBucket;
    }

    public ResourcesBucket getAzureResourcesBucket() {
        return azureResourcesBucket;
    }

    public DomainConfiguration getDomainConfig(String domain) {
        if (domain == null) {
            return null;
        }
        return allResourcesBucket.getConfigurationByDomain().get(domain);
    }

    @PostConstruct
    public void init() {
        ResourcesBucket persisted = azureResourcesBucketDao.load();
        if (persisted != null) {
            azureResourcesBucket = persisted;
        }

        mergeAllResourcesBucket();
    }

    private void mergeAllResourcesBucket() {
        allResourcesBucket = azureResourcesBucket;
    }

    private void persist() {
        azureResourcesBucketDao.save(azureResourcesBucket);
    }

    public void setAzureResourcesBucket(ResourcesBucket azureResourcesBucket) {
        this.azureResourcesBucket = azureResourcesBucket;
        mergeAllResourcesBucket();
        persist();
    }

    public void updateAzureResourcesBucket(Consumer<ResourcesBucket> consumer) {
        consumer.accept(azureResourcesBucket);
        setAzureResourcesBucket(azureResourcesBucket);
    }

}
