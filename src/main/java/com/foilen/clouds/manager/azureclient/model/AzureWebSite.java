/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

public class AzureWebSite extends BaseApiResponseWithError {

    private String id;
    private String name;
    private String location;
    private AzureWebSiteProperties properties;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public AzureWebSiteProperties getProperties() {
        return properties;
    }

    public void setProperties(AzureWebSiteProperties properties) {
        this.properties = properties;
    }
}
