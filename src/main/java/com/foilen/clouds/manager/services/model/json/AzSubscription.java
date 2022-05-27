/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.json;

import com.foilen.smalltools.restapi.model.AbstractApiBase;

public class AzSubscription extends AbstractApiBase {

    private String id;
    private String name;
    private String tenantId;
    private AzSubscriptionUser user;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getTenantId() {
        return tenantId;
    }

    public AzSubscriptionUser getUser() {
        return user;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setUser(AzSubscriptionUser user) {
        this.user = user;
    }

}
