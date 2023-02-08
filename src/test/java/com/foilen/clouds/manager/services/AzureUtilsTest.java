/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import org.junit.Assert;
import org.junit.Test;

public class AzureUtilsTest {

    @Test
    public void getAzIdDetails_1() {
        var details = AzureUtils.getAzIdDetails("/subscriptions/mySubId/resourceGroups/myRG/providers/Microsoft.Web/sites/myName/config");
        Assert.assertEquals("mySubId", details.getSubscriptionId());
        Assert.assertEquals("myRG", details.getResourceGroupName());
        Assert.assertEquals("myName", details.getName());
    }

    @Test

    public void getAzIdDetails_2() {
        var details = AzureUtils.getAzIdDetails("/subscriptions/mySubId/resourceGroups/myRG/providers/Microsoft.Web/sites/myName");
        Assert.assertEquals("mySubId", details.getSubscriptionId());
        Assert.assertEquals("myRG", details.getResourceGroupName());
        Assert.assertEquals("myName", details.getName());
    }
}