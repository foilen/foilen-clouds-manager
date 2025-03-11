/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

public class DigitalOceanDnsZone extends CommonResource implements DnsZone {

    public DigitalOceanDnsZone() {
        super(CloudProvider.DIGITAL_OCEAN);
    }

    @Override
    public String getName() {
        return getId();
    }

    public void setName(String name) {
        setId(name);
    }

    @Override
    public String getRegionId() {
        return null;
    }

    @Override
    public void setRegionId(String region) {
    }

}
