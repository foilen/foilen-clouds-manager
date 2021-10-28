/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.foilen.clouds.manager.services.model.AzureResourceGroup;
import com.foilen.clouds.manager.services.model.CloudProviderInfo;
import com.foilen.clouds.manager.services.model.IdInfo;
import com.foilen.clouds.manager.services.model.NameInfo;
import com.foilen.clouds.manager.services.model.RegionInfo;
import com.foilen.smalltools.tools.AbstractBasics;
import com.google.common.base.Joiner;

@Component
public class ConsoleDisplayService extends AbstractBasics implements DisplayService {

    @Override
    public void display(String text) {
        System.out.println(text);
    }

    @Override
    public void displayResource(int indentation, Object resource) {
        for (int i = 0; i < indentation; ++i) {
            System.out.print('\t');
        }

        List<String> details = new ArrayList<>();

        if (resource instanceof CloudProviderInfo) {
            details.add(((CloudProviderInfo) resource).getProvider().toString());
        }
        if (resource instanceof NameInfo) {
            details.add(((NameInfo) resource).getName());
        }
        if (resource instanceof RegionInfo) {
            details.add("(" + ((RegionInfo) resource).getRegion() + ")");
        }
        if (resource instanceof AzureResourceGroup) {
            details.add("[" + ((AzureResourceGroup) resource).getResourceGroup() + "]");
        }
        if (resource instanceof IdInfo) {
            details.add(((IdInfo) resource).getId());
        }

        details.removeIf(it -> it == null);

        System.out.println(Joiner.on(' ').join(details));

    }

}
