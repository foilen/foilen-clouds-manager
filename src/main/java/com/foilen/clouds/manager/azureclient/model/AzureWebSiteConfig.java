/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

public class AzureWebSiteConfig extends BaseApiResponse {

    private String id;
    private String name;
    private String location;
    private AzureWebSiteConfigProperties properties;

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

    public AzureWebSiteConfigProperties getProperties() {
        return properties;
    }

    public void setProperties(AzureWebSiteConfigProperties properties) {
        this.properties = properties;
    }
}
