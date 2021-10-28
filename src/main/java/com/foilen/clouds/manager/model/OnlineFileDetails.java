/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.model;

import com.foilen.smalltools.tools.AbstractBasics;

public class OnlineFileDetails extends AbstractBasics {

    private String version;
    private String jarUrl;

    public String getJarUrl() {
        return jarUrl;
    }

    public String getVersion() {
        return version;
    }

    public OnlineFileDetails setJarUrl(String jarUrl) {
        this.jarUrl = jarUrl;
        return this;
    }

    public OnlineFileDetails setVersion(String version) {
        this.version = version;
        return this;
    }

}
