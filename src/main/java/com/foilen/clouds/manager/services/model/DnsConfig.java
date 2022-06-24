package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;

public class DnsConfig extends AbstractBasics {

    private boolean startEmpty = true;
    private List<DnsEntryConfig> configs;

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
