/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

public class AzureDnsZone extends CommonResource implements DnsZone, AzureResourceGroup {

    public static AzureDnsZone from(com.azure.resourcemanager.dns.models.DnsZone dnsZone) {
        AzureDnsZone azureDnsZone = new AzureDnsZone();
        azureDnsZone.setId(dnsZone.id());
        azureDnsZone.setName(dnsZone.name());
        azureDnsZone.setResourceGroup(dnsZone.resourceGroupName());
        azureDnsZone.setRegion(dnsZone.regionName());
        return azureDnsZone;
    }

    private String resourceGroup;
    private String region;
    private String name;

    public AzureDnsZone() {
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
