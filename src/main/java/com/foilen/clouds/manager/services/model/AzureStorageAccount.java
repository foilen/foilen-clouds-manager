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

public class AzureStorageAccount extends CommonResource implements StorageAccount, HasResourceGroup {

    private String resourceGroup;
    private String region;
    private String name;
    private String skuName;
    private Boolean largeFileShares;

    public AzureStorageAccount() {
        super(CloudProvider.AZURE);
    }

    public static AzureStorageAccount from(com.azure.resourcemanager.storage.models.StorageAccount storageAccount) {
        var item = new AzureStorageAccount();
        item.setId(storageAccount.id());
        item.name = storageAccount.name();
        item.resourceGroup = storageAccount.resourceGroupName();
        item.region = storageAccount.regionName();
        item.skuName = storageAccount.skuType().name().toString();
        item.largeFileShares = storageAccount.isLargeFileSharesEnabled();
        return item;
    }

    public List<String> differences(AzureStorageAccount current) {
        var differences = new ArrayList<String>();
        different(differences, "Storage Account", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Storage Account", name, "region", region, current.region);
        different(differences, "Storage Account", name, "skuName", skuName, current.skuName);
        different(differences, "Storage Account", name, "largeFileShares", largeFileShares, current.largeFileShares);
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
    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public Boolean getLargeFileShares() {
        return largeFileShares;
    }

    public void setLargeFileShares(Boolean largeFileShares) {
        this.largeFileShares = largeFileShares;
    }
}
