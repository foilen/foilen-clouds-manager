/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;


import java.util.ArrayList;
import java.util.List;

public class AzureStorageAccount extends CommonResource implements StorageAccount, HasResourceGroup {

    private String resourceGroup;
    private String regionId;
    private String name;
    private String skuName;
    private Boolean largeFileShares;

    private List<AzureStorageFileShare> azureFileShares = new ArrayList<>();

    public AzureStorageAccount() {
        super(CloudProvider.AZURE);
    }

    public static AzureStorageAccount from(com.azure.resourcemanager.storage.models.StorageAccount storageAccount) {
        var item = new AzureStorageAccount();
        item.setId(storageAccount.id());
        item.name = storageAccount.name();
        item.resourceGroup = storageAccount.resourceGroupName();
        item.regionId = storageAccount.regionName();
        item.skuName = storageAccount.skuType().name().toString();
        item.largeFileShares = storageAccount.isLargeFileSharesEnabled();
        return item;
    }

    public List<String> differences(AzureStorageAccount current) {
        var differences = new ArrayList<String>();
        different(differences, "Storage Account", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Storage Account", name, "regionId", regionId, current.regionId);
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

    public List<AzureStorageFileShare> getAzureFileShares() {
        return azureFileShares;
    }

    public void setAzureFileShares(List<AzureStorageFileShare> azureFileShares) {
        this.azureFileShares = azureFileShares;
    }
}
