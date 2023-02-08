/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

public class AzureApplicationServiceCertificateRequestProperties extends BaseApiResponse {

    private String canonicalName;
    private String domainValidationMethod;
    private String serverFarmId;
    private String password = "";

    public String getCanonicalName() {
        return canonicalName;
    }

    public AzureApplicationServiceCertificateRequestProperties setCanonicalName(String canonicalName) {
        this.canonicalName = canonicalName;
        return this;
    }

    public String getDomainValidationMethod() {
        return domainValidationMethod;
    }

    public AzureApplicationServiceCertificateRequestProperties setDomainValidationMethod(String domainValidationMethod) {
        this.domainValidationMethod = domainValidationMethod;
        return this;
    }

    public String getServerFarmId() {
        return serverFarmId;
    }

    public AzureApplicationServiceCertificateRequestProperties setServerFarmId(String serverFarmId) {
        this.serverFarmId = serverFarmId;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AzureApplicationServiceCertificateRequestProperties setPassword(String password) {
        this.password = password;
        return this;
    }
}
