/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient.model;

import com.foilen.smalltools.restapi.model.AbstractApiBaseWithAdditionalProperties;

public class DigitalOceanDomain extends AbstractApiBaseWithAdditionalProperties {

    private String name;
    private long ttl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTtl() {
        return ttl;
    }

    public void setTtl(long ttl) {
        this.ttl = ttl;
    }
}
