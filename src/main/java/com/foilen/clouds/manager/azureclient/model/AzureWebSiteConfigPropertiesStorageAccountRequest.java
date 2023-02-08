/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

import java.util.HashMap;
import java.util.Map;

public class AzureWebSiteConfigPropertiesStorageAccountRequest extends BaseApiResponse {

    private Map<String, AzureWebSiteConfigPropertiesStorageAccountRequestProperties> properties = new HashMap<>();

    public Map<String, AzureWebSiteConfigPropertiesStorageAccountRequestProperties> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, AzureWebSiteConfigPropertiesStorageAccountRequestProperties> properties) {
        this.properties = properties;
    }
}
