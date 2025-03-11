/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands.model;

import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;

public class ClearDnsEntry extends AbstractBasics implements Comparable<ClearDnsEntry> {

    private String name;
    private String type;

    @Override
    public int compareTo(ClearDnsEntry o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(name, o.name);
        cc = cc.compare(type, o.type);
        return cc.result();
    }

    public String getName() {
        return name;
    }

    public ClearDnsEntry setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public ClearDnsEntry setType(String type) {
        this.type = type;
        return this;
    }

    @Override
    public String toString() {
        return "ClearDnsEntry{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

}
