/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

import java.util.Map;

public class AzureWebSiteConfigProperties extends BaseApiResponse {

    private Map<String, AzureWebSiteConfigPropertiesStorageAccount> azureStorageAccounts;

    public Map<String, AzureWebSiteConfigPropertiesStorageAccount> getAzureStorageAccounts() {
        return azureStorageAccounts;
    }

    public void setAzureStorageAccounts(Map<String, AzureWebSiteConfigPropertiesStorageAccount> azureStorageAccounts) {
        this.azureStorageAccounts = azureStorageAccounts;
    }
}
