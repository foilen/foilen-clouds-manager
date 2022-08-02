/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.manageconfig;

import com.foilen.clouds.manager.services.model.AzureDnsZone;
import com.foilen.clouds.manager.services.model.DnsConfig;
import com.foilen.smalltools.tools.AbstractBasics;

public class AzureDnsZoneManageConfiguration extends AbstractBasics {

    private AzureDnsZone resource;
    private DnsConfig config;

    public AzureDnsZone getResource() {
        return resource;
    }

    public AzureDnsZoneManageConfiguration setResource(AzureDnsZone resource) {
        this.resource = resource;
        return this;
    }

    public DnsConfig getConfig() {
        return config;
    }

    public AzureDnsZoneManageConfiguration setConfig(DnsConfig config) {
        this.config = config;
        return this;
    }
}
