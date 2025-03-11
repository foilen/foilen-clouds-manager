/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.smalltools.tools.AbstractBasics;

public class AzIdDetails extends AbstractBasics {

    private String subscriptionId;
    private String resourceGroupName;
    private String name;

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getResourceGroupName() {
        return resourceGroupName;
    }

    public void setResourceGroupName(String resourceGroupName) {
        this.resourceGroupName = resourceGroupName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
