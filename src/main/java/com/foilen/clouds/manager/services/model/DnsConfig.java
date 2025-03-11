/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;
import java.util.Set;

public class DnsConfig extends AbstractBasics {

    private boolean startEmpty = true;
    private Set<String> startWithDomains;
    private List<DnsEntryConfig> configs;

    public Set<String> getStartWithDomains() {
        return startWithDomains;
    }

    public void setStartWithDomains(Set<String> startWithDomains) {
        this.startWithDomains = startWithDomains;
    }

    public List<DnsEntryConfig> getConfigs() {
        return configs;
    }

    public DnsConfig setConfigs(List<DnsEntryConfig> configs) {
        this.configs = configs;
        return this;
    }

    public boolean isStartEmpty() {
        return startEmpty;
    }

    public DnsConfig setStartEmpty(boolean startEmpty) {
        this.startEmpty = startEmpty;
        return this;
    }
}
