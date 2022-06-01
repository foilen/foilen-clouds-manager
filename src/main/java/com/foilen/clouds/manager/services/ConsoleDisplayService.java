/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.foilen.clouds.manager.services.model.HasResourceGroup;
import com.foilen.clouds.manager.services.model.HasCloudProvider;
import com.foilen.clouds.manager.services.model.HasId;
import com.foilen.clouds.manager.services.model.HasName;
import com.foilen.clouds.manager.services.model.HasRegion;
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

        if (resource instanceof HasCloudProvider) {
            details.add(((HasCloudProvider) resource).getProvider().toString());
        }
        if (resource instanceof HasName) {
            details.add(((HasName) resource).getName());
        }
        if (resource instanceof HasRegion) {
            details.add("(" + ((HasRegion) resource).getRegion() + ")");
        }
        if (resource instanceof HasResourceGroup) {
            details.add("[" + ((HasResourceGroup) resource).getResourceGroup() + "]");
        }
        if (resource instanceof HasId) {
            details.add(((HasId) resource).getId());
        }

        details.removeIf(it -> it == null);

        System.out.println(Joiner.on(' ').join(details));

    }

}
