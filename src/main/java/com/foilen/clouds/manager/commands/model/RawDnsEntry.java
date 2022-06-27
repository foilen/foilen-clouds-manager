/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands.model;

import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.collect.ComparisonChain;

public class RawDnsEntry extends AbstractBasics implements Comparable<RawDnsEntry> {

    private String name;
    private String type;
    private String details;

    private Integer priority;
    private Integer weight;
    private Integer port;

    private long ttl = 60 * 60 * 24 * 2; // 2 Days

    public RawDnsEntry() {
    }

    @Override
    public int compareTo(RawDnsEntry o) {
        ComparisonChain cc = ComparisonChain.start();
        cc = cc.compare(name, o.name);
        cc = cc.compare(type, o.type);
        cc = cc.compare(details, o.details);
        cc = cc.compare(ttl, o.ttl);
        cc = cc.compare(priority == null ? Integer.valueOf(0) : priority, o.priority == null ? Integer.valueOf(0) : o.priority);
        cc = cc.compare(weight == null ? Integer.valueOf(0) : weight, o.weight == null ? Integer.valueOf(0) : o.weight);
        cc = cc.compare(port == null ? Integer.valueOf(0) : port, o.port == null ? Integer.valueOf(0) : o.port);
        return cc.result();
    }

    public String getDetails() {
        return details;
    }

    public RawDnsEntry setDetails(String details) {
        this.details = details;
        return this;
    }

    public String getName() {
        return name;
    }

    public RawDnsEntry setName(String name) {
        this.name = name;
        return this;
    }

    public Integer getPort() {
        return port;
    }

    public RawDnsEntry setPort(Integer port) {
        this.port = port;
        return this;
    }

    public Integer getPriority() {
        return priority;
    }

    public RawDnsEntry setPriority(Integer priority) {
        this.priority = priority;
        return this;
    }

    public long getTtl() {
        return ttl;
    }

    public RawDnsEntry setTtl(long ttl) {
        this.ttl = ttl;
        return this;
    }

    public String getType() {
        return type;
    }

    public RawDnsEntry setType(String type) {
        this.type = type;
        return this;
    }

    public Integer getWeight() {
        return weight;
    }

    public RawDnsEntry setWeight(Integer weight) {
        this.weight = weight;
        return this;
    }

    @Override
    public String toString() {
        return "RawDnsEntry{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", details='" + details + '\'' +
                ", ttl=" + ttl +
                ", priority=" + priority +
                ", weight=" + weight +
                ", port=" + port +
                '}';
    }

}
