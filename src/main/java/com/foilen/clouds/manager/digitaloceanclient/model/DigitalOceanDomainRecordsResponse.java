/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DigitalOceanDomainRecordsResponse extends BaseApiResponseWithError {

    @JsonProperty("domain_records")
    private List<DigitalOceanDomainRecord> domainRecords = new ArrayList<>();

    public List<DigitalOceanDomainRecord> getDomainRecords() {
        return domainRecords;
    }

    public void setDomainRecords(List<DigitalOceanDomainRecord> domainRecords) {
        this.domainRecords = domainRecords;
    }

}
