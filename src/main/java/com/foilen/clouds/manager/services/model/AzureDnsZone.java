/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import java.util.ArrayList;
import java.util.List;

public class AzureDnsZone extends CommonResource implements DnsZone, HasResourceGroup {

    private String resourceGroup;
    private String regionId;
    private String name;

    public AzureDnsZone() {
        super(CloudProvider.AZURE);
    }

    public static AzureDnsZone from(com.azure.resourcemanager.dns.models.DnsZone dnsZone) {
        var item = new AzureDnsZone();
        item.setId(dnsZone.id());
        item.setName(dnsZone.name());
        item.setResourceGroup(dnsZone.resourceGroupName());
        item.setRegionId(dnsZone.regionName());
        return item;
    }

    public List<String> differences(AzureDnsZone current) {
        var differences = new ArrayList<String>();
        different(differences, "Dns Zone", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Dns Zone", name, "regionId", regionId, current.regionId);
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

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

}
