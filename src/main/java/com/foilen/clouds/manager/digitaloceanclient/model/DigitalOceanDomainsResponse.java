/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient.model;

import java.util.ArrayList;
import java.util.List;

public class DigitalOceanDomainsResponse extends BaseApiResponseWithError {

    private List<DigitalOceanDomain> domains = new ArrayList<>();

    public List<DigitalOceanDomain> getDomains() {
        return domains;
    }

    public void setDomains(List<DigitalOceanDomain> domains) {
        this.domains = domains;
    }

}
