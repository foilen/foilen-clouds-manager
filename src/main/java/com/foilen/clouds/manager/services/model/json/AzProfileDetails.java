/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.json;

import com.foilen.smalltools.restapi.model.AbstractApiBase;

import java.util.ArrayList;
import java.util.List;

public class AzProfileDetails extends AbstractApiBase {

    private List<AzSubscription> subscriptions = new ArrayList<>();

    public List<AzSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<AzSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

}
