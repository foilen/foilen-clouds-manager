/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import java.util.HashSet;
import java.util.Set;

import com.foilen.smalltools.tools.AbstractBasics;

public class DomainConfiguration extends AbstractBasics {

    private String domainName;

    private Set<DnsZone> dnsZones = new HashSet<>();

    private Set<WebApp> httpWebApp = new HashSet<>();
    private Set<WebApp> httpsWebApp = new HashSet<>();

    public Set<DnsZone> getDnsZones() {
        return dnsZones;
    }

    public String getDomainName() {
        return domainName;
    }

    public Set<WebApp> getHttpsWebApp() {
        return httpsWebApp;
    }

    public Set<WebApp> getHttpWebApp() {
        return httpWebApp;
    }

    public void setDnsZones(Set<DnsZone> dnsZones) {
        this.dnsZones = dnsZones;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setHttpsWebApp(Set<WebApp> httpsWebApp) {
        this.httpsWebApp = httpsWebApp;
    }

    public void setHttpWebApp(Set<WebApp> httpWebApp) {
        this.httpWebApp = httpWebApp;
    }

}
