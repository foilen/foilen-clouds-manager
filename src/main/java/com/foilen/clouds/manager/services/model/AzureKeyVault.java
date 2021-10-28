/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.keyvault.models.Vault;

public class AzureKeyVault extends CommonResource implements SecretStore, AzureResourceGroup, RegionInfo {

    public static AzureKeyVault from(Vault vault) {
        AzureKeyVault azureKeyVault = new AzureKeyVault();
        azureKeyVault.setId(vault.id());
        azureKeyVault.setName(vault.name());
        azureKeyVault.setResourceGroup(vault.resourceGroupName());
        azureKeyVault.setRegion(vault.regionName());
        return azureKeyVault;
    }

    private String name;
    private String resourceGroup;
    private String region;

    public AzureKeyVault() {
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
