/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

public class AzureWebSiteProperties extends BaseApiResponse {

    private String customDomainVerificationId;

    public String getCustomDomainVerificationId() {
        return customDomainVerificationId;
    }

    public void setCustomDomainVerificationId(String customDomainVerificationId) {
        this.customDomainVerificationId = customDomainVerificationId;
    }
}
