/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.manageconfig;

import com.foilen.clouds.manager.services.model.AzureWebApp;

public class AzureWebAppManageConfiguration {

    private AzureWebApp resource;

    public AzureWebApp getResource() {
        return resource;
    }

    public AzureWebAppManageConfiguration setResource(AzureWebApp resource) {
        this.resource = resource;
        return this;
    }

}
