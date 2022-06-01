/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.appservice.models.WebAppBasic;

public class AzureWebApp extends CommonResource implements WebApp, HasResourceGroup {

    public static WebApp from(WebAppBasic webApp) {
        AzureWebApp azureWebApp = new AzureWebApp();
        azureWebApp.setId(webApp.id());
        azureWebApp.setName(webApp.name());
        azureWebApp.setResourceGroup(webApp.resourceGroupName());
        azureWebApp.setRegion(webApp.regionName());
        return azureWebApp;
    }

    private String resourceGroup;
    private String region;
    private String name;

    public AzureWebApp() {
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
