/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.resources.models.ResourceGroup;

public class AzureResourceGroup extends CommonResource implements DnsZone {

    public static AzureResourceGroup from(ResourceGroup resourceGroup) {
        var item = new AzureResourceGroup();
        item.setId(resourceGroup.id());
        item.setName(resourceGroup.name());
        item.setRegion(resourceGroup.regionName());
        return item;
    }

    private String region;
    private String name;

    public AzureResourceGroup() {
        super(CloudProvider.AZURE);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRegion() {
        return region;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(String region) {
        this.region = region;
    }

}
