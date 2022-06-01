/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.services.model.ManageConfiguration;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ManageService extends AbstractBasics {

    @Autowired
    private CloudAzureService cloudAzureService;

    public void manage(String file) {

        var config = JsonTools.readFromFile(file, ManageConfiguration.class);
        manage(config);
    }

    public void manage(ManageConfiguration config) {
        config.getAzureResourceGroups().forEach(it -> {
            cloudAzureService.resourceGroupManage(it);
        });
        config.getAzureKeyVaults().forEach(it -> {
            cloudAzureService.keyVaultManage(config, it);
        });
    }
}
