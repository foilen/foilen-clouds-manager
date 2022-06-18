/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.services.model.AzureMariadbManageConfiguration;
import com.foilen.clouds.manager.services.model.ManageConfiguration;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        config.getAzureApplicationServicePlans().forEach(it -> {
            cloudAzureService.applicationServicePlanManage(config, it);
        });
        config.getAzureMariadbs().forEach(it -> {
            cloudAzureService.mariadbManage(config, it);
        });
    }

    public void export(String file) {
        ManageConfiguration config = new ManageConfiguration();

        logger.info("Getting resource groups");
        config.setAzureResourceGroups(cloudAzureService.resourceGroupFindAll());

        logger.info("Getting key vaults");
        config.setAzureKeyVaults(cloudAzureService.keyVaultFindAll());

        logger.info("Getting application service plans");
        config.setAzureApplicationServicePlans(cloudAzureService.applicationServicePlansFindAll());

        logger.info("Getting mariadbs");
        config.setAzureMariadbs(cloudAzureService.mariadbList().stream()
                .map(it -> new AzureMariadbManageConfiguration().setResource(it))
                .collect(Collectors.toList())
        );

        logger.info("Export to {}", file);
        JsonTools.writeToFile(file, cleanup(config));
    }

    private Object cleanup(ManageConfiguration config) {
        var cloned = JsonTools.clone(config, Map.class);
        cleanup(cloned);
        return cloned;
    }

    private void cleanup(Map cloned) {

        cloned.remove("id");
        cloned.remove("provider");

        cloned.forEach((key, value) -> {
            if (value instanceof Map) {
                cleanup((Map) value);
            }
            if (value instanceof List) {
                ((List<?>) value).forEach(it -> {
                    if (it instanceof Map) {
                        cleanup((Map) it);
                    }
                });
            }
        });
    }
}
