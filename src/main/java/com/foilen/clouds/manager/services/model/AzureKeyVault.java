/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.keyvault.models.Vault;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class AzureKeyVault extends CommonResource implements SecretStore, HasResourceGroup, HasRegion {

    private String name;
    private String resourceGroup;
    private String regionId;

    public AzureKeyVault() {
        super(CloudProvider.AZURE);
    }

    public static AzureKeyVault from(Vault vault) {
        var item = new AzureKeyVault();
        item.setId(vault.id());
        item.name = vault.name();
        item.resourceGroup = vault.resourceGroupName();
        item.regionId = vault.regionName();
        return item;
    }

    public List<String> differences(AzureKeyVault current) {
        var differences = new ArrayList<String>();
        different(differences, "Key Vault", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Key Vault", name, "regionId", regionId, current.regionId);
        return differences;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AzureKeyVault) {
            return Objects.equals(getId(), ((AzureKeyVault) obj).getId());
        }
        return false;
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

    @Override
    public int hashCode() {
        return Objects.hash(getProvider(), getId());
    }

}
