/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.mariadb.models.Server;

public class AzureMariadb extends CommonResource implements Mariadb, AzureResourceGroup {

    public static AzureMariadb from(Server server) {
        var item = new AzureMariadb();
        item.setId(server.id());
        item.setName(server.name());
        if (item.getId() != null) {
            var parts = item.getId().split("/");
            if (parts.length > 3) {
                item.setResourceGroup(parts[4]);
            }
        }
        item.setRegion(server.regionName());
        return item;
    }

    private String resourceGroup;
    private String region;
    private String name;

    public AzureMariadb() {
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
