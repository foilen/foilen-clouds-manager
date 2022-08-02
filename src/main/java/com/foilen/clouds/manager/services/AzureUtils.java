/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.smalltools.tools.AssertTools;

public class AzureUtils {

    public static AzIdDetails getAzIdDetails(String id) {
        var details = new AzIdDetails();
        var parts = id.split("/");
        AssertTools.assertTrue(parts.length >= 8);
        details.setSubscriptionId(parts[2]);
        details.setResourceGroupName(parts[4]);
        details.setName(parts[8]);
        return details;
    }

}
