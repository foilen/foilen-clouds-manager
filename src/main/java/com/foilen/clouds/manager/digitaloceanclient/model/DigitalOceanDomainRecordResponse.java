/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DigitalOceanDomainRecordResponse extends BaseApiResponseWithError {

    @JsonProperty("domain_record")
    private DigitalOceanDomainRecord domainRecord;

    public DigitalOceanDomainRecord getDomainRecord() {
        return domainRecord;
    }

    public void setDomainRecord(DigitalOceanDomainRecord domainRecord) {
        this.domainRecord = domainRecord;
    }

}
