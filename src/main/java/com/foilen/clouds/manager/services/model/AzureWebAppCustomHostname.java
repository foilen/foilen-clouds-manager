/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

public class AzureWebAppCustomHostname extends AbstractBasics {

    private String domainName;
    private boolean createCertificate;

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public boolean isCreateCertificate() {
        return createCertificate;
    }

    public void setCreateCertificate(boolean createCertificate) {
        this.createCertificate = createCertificate;
    }

}
