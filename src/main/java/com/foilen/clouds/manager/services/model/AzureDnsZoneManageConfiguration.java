/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

public class AzureDnsZoneManageConfiguration {

    private AzureDnsZone resource;

    public AzureDnsZone getResource() {
        return resource;
    }

    public AzureDnsZoneManageConfiguration setResource(AzureDnsZone resource) {
        this.resource = resource;
        return this;
    }

}
