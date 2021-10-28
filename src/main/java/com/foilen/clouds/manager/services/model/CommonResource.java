/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

public abstract class CommonResource extends AbstractBasics implements CloudProviderInfo {

    private CloudProvider provider;

    private String id;

    public CommonResource(CloudProvider provider) {
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    @Override
    public CloudProvider getProvider() {
        return provider;
    }

    public void setId(String id) {
        this.id = id;
    }

}
