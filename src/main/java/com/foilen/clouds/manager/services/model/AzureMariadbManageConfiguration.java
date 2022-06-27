/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.databasetools.manage.mariadb.MariadbManagerConfig;

public class AzureMariadbManageConfiguration {

    private AzureMariadb resource;
    private MariadbManagerConfig config;

    public AzureMariadb getResource() {
        return resource;
    }

    public AzureMariadbManageConfiguration setResource(AzureMariadb resource) {
        this.resource = resource;
        return this;
    }

    public MariadbManagerConfig getConfig() {
        return config;
    }

    public AzureMariadbManageConfiguration setConfig(MariadbManagerConfig config) {
        this.config = config;
        return this;
    }
}
