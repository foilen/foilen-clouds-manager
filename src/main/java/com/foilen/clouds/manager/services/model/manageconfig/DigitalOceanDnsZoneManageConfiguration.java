/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.manageconfig;

import com.foilen.clouds.manager.services.model.DigitalOceanDnsZone;
import com.foilen.clouds.manager.services.model.DnsConfig;
import com.foilen.smalltools.tools.AbstractBasics;

public class DigitalOceanDnsZoneManageConfiguration extends AbstractBasics {

    private DigitalOceanDnsZone resource;
    private DnsConfig config;

    public DigitalOceanDnsZone getResource() {
        return resource;
    }

    public DigitalOceanDnsZoneManageConfiguration setResource(DigitalOceanDnsZone resource) {
        this.resource = resource;
        return this;
    }

    public DnsConfig getConfig() {
        return config;
    }

    public DigitalOceanDnsZoneManageConfiguration setConfig(DnsConfig config) {
        this.config = config;
        return this;
    }
}
