/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

public class AzureWebSiteConfigPropertiesStorageAccountRequestProperties extends BaseApiResponse {

    private String type = "AzureFiles";
    private String accountName;
    private String shareName;
    private String mountPath;
    private String accessKey;

    public String getAccountName() {
        return accountName;
    }

    public AzureWebSiteConfigPropertiesStorageAccountRequestProperties setAccountName(String accountName) {
        this.accountName = accountName;
        return this;
    }

    public String getShareName() {
        return shareName;
    }

    public AzureWebSiteConfigPropertiesStorageAccountRequestProperties setShareName(String shareName) {
        this.shareName = shareName;
        return this;
    }

    public String getMountPath() {
        return mountPath;
    }

    public AzureWebSiteConfigPropertiesStorageAccountRequestProperties setMountPath(String mountPath) {
        this.mountPath = mountPath;
        return this;
    }

    public String getType() {
        return type;
    }

    public AzureWebSiteConfigPropertiesStorageAccountRequestProperties setType(String type) {
        this.type = type;
        return this;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public AzureWebSiteConfigPropertiesStorageAccountRequestProperties setAccessKey(String accessKey) {
        this.accessKey = accessKey;
        return this;
    }
}
