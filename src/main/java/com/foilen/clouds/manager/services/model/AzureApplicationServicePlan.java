/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.appservice.models.AppServicePlan;

import java.util.ArrayList;
import java.util.List;

public class AzureApplicationServicePlan extends CommonResource implements DnsZone, HasResourceGroup, HasRegion {

    private String resourceGroup;
    private String regionId;
    private String name;
    private String pricingTierSize;
    private String operatingSystem;
    private Integer capacity;
    private Boolean perSiteScaling;

    public AzureApplicationServicePlan() {
        super(CloudProvider.AZURE);
    }

    public static AzureApplicationServicePlan from(AppServicePlan appServicePlan) {
        var item = new AzureApplicationServicePlan();
        item.setId(appServicePlan.id());
        item.name = appServicePlan.name();
        item.resourceGroup = appServicePlan.resourceGroupName();
        item.regionId = appServicePlan.region().name();

        item.pricingTierSize = appServicePlan.pricingTier().toSkuDescription().size();
        item.operatingSystem = appServicePlan.operatingSystem().name();
        item.capacity = appServicePlan.capacity();
        item.perSiteScaling = appServicePlan.perSiteScaling();
        return item;
    }

    public List<String> differences(AzureApplicationServicePlan current) {
        var differences = new ArrayList<String>();
        different(differences, "Application Service Plan", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Application Service Plan", name, "regionId", regionId, current.regionId);
        different(differences, "Application Service Plan", name, "pricingTierSize", pricingTierSize, current.pricingTierSize);
        different(differences, "Application Service Plan", name, "operatingSystem", operatingSystem, current.operatingSystem);
        different(differences, "Application Service Plan", name, "capacity", capacity, current.capacity);
        different(differences, "Application Service Plan", name, "perSiteScaling", perSiteScaling, current.perSiteScaling);
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

    public String getPricingTierSize() {
        return pricingTierSize;
    }

    public void setPricingTierSize(String pricingTierSize) {
        this.pricingTierSize = pricingTierSize;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Boolean getPerSiteScaling() {
        return perSiteScaling;
    }

    public void setPerSiteScaling(Boolean perSiteScaling) {
        this.perSiteScaling = perSiteScaling;
    }

}
