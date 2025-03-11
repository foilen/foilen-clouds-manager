/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

public class AzureCustomDomainDnsEntry extends AbstractBasics {

    private String webappResourceGroupName;
    private String webappName;
    private String hostname;
    private boolean useCname = true;

    public String getWebappResourceGroupName() {
        return webappResourceGroupName;
    }

    public void setWebappResourceGroupName(String webappResourceGroupName) {
        this.webappResourceGroupName = webappResourceGroupName;
    }

    public String getWebappName() {
        return webappName;
    }

    public void setWebappName(String webappName) {
        this.webappName = webappName;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public boolean isUseCname() {
        return useCname;
    }

    public void setUseCname(boolean useCname) {
        this.useCname = useCname;
    }
}
