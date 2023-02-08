/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;


public class AzureApplicationServiceCertificateRequest extends BaseApiResponse {

    private String location;
    private AzureApplicationServiceCertificateRequestProperties properties;

    public String getLocation() {
        return location;
    }

    public AzureApplicationServiceCertificateRequest setLocation(String location) {
        this.location = location;
        return this;
    }

    public AzureApplicationServiceCertificateRequestProperties getProperties() {
        return properties;
    }

    public AzureApplicationServiceCertificateRequest setProperties(AzureApplicationServiceCertificateRequestProperties properties) {
        this.properties = properties;
        return this;
    }
}
