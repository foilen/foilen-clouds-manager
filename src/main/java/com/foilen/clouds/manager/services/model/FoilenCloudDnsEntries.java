package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

public class FoilenCloudDnsEntries extends AbstractBasics {

    private String infraBaseUrl;
    private String apiUser;
    private String apiKey;

    private String domainName;

    public String getInfraBaseUrl() {
        return infraBaseUrl;
    }

    public void setInfraBaseUrl(String infraBaseUrl) {
        this.infraBaseUrl = infraBaseUrl;
    }

    public String getApiUser() {
        return apiUser;
    }

    public void setApiUser(String apiUser) {
        this.apiUser = apiUser;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }
}
