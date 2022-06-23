/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import java.util.ArrayList;
import java.util.List;

public class AzureDnsZone extends CommonResource implements DnsZone, HasResourceGroup {

    public static AzureDnsZone from(com.azure.resourcemanager.dns.models.DnsZone dnsZone) {
        var item = new AzureDnsZone();
        item.setId(dnsZone.id());
        item.setName(dnsZone.name());
        item.setResourceGroup(dnsZone.resourceGroupName());
        item.setRegion(dnsZone.regionName());
        return item;
    }

    private String resourceGroup;
    private String region;
    private String name;

    public AzureDnsZone() {
        super(CloudProvider.AZURE);
    }

    public List<String> differences(AzureDnsZone current) {
        var differences = new ArrayList<String>();
        different(differences,"Dns Zone", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences,"Dns Zone", name, "region", region, current.region);
        return differences;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

}
