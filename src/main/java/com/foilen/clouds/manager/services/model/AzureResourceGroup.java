/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.resources.models.ResourceGroup;

import java.util.ArrayList;
import java.util.List;

public class AzureResourceGroup extends CommonResource implements DnsZone {

    private String regionId;
    private String name;

    public AzureResourceGroup() {
        super(CloudProvider.AZURE);
    }

    public static AzureResourceGroup from(ResourceGroup resourceGroup) {
        var item = new AzureResourceGroup();
        item.setId(resourceGroup.id());
        item.name = resourceGroup.name();
        item.regionId = resourceGroup.regionName();
        return item;
    }

    public List<String> differences(AzureResourceGroup current) {
        var differences = new ArrayList<String>();
        different(differences, "Resource Group", name, "regionId", regionId, current.regionId);
        return differences;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

}
