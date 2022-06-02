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
import java.util.Objects;

import com.azure.resourcemanager.keyvault.models.Vault;

public class AzureKeyVault extends CommonResource implements SecretStore, HasResourceGroup, HasRegion {

    public static AzureKeyVault from(Vault vault) {
        var item = new AzureKeyVault();
        item.setId(vault.id());
        item.name = vault.name();
        item.resourceGroup = vault.resourceGroupName();
        item.region = vault.region().name();
        return item;
    }

    private String name;
    private String resourceGroup;
    private String region;

    public AzureKeyVault() {
        super(CloudProvider.AZURE);
    }

    public List<String> differences(AzureKeyVault current) {
        var differences = new ArrayList<String>();
        different(differences,"Key Vault", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences,"Key Vault", name, "region", region, current.region);
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

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProvider(), getId());
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
