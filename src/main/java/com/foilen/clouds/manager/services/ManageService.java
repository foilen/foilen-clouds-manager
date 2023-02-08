/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.services.model.ConflictResolution;
import com.foilen.clouds.manager.services.model.DnsConfig;
import com.foilen.clouds.manager.services.model.DnsEntryConfig;
import com.foilen.clouds.manager.services.model.manageconfig.*;
import com.foilen.smalltools.tools.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class ManageService extends AbstractBasics {

    @Autowired
    private CloudAzureService cloudAzureService;

    public void manage(String file) {

        try {
            var config = JsonTools.readFromFile(file, ManageConfiguration.class);
            manage(config);
        } catch (Exception e) {
            logger.error("Problem managing the resources", e);
        }
    }

    public void manage(ManageConfiguration config) {

        var globalContext = new ManageContext();

        boolean retry = true;
        var lastRetryHash = "";
        var retrySameHashCount = 0;
        while (retry) {

            retry = false;

            var currentContext = new ManageContext();
            config.getAzureResourceGroups().forEach(it -> cloudAzureService.resourceGroupManage(currentContext, it));
            config.getAzureKeyVaults().forEach(it -> cloudAzureService.keyVaultManage(currentContext, config, it));
            config.getAzureApplicationServicePlans().forEach(it -> cloudAzureService.applicationServicePlanManage(currentContext, config, it));
            config.getAzureMariadbs().forEach(it -> cloudAzureService.mariadbManage(currentContext, config, it));
            config.getAzureDnsZones().forEach(it -> cloudAzureService.dnsZoneManage(currentContext, config, it));
            config.getAzureStorageAccounts().forEach(it -> cloudAzureService.storageAccountManage(currentContext, config, it));
            config.getAzureWebapps().forEach(it -> cloudAzureService.webappManage(currentContext, config, it));

            // Retry logic
            if (!currentContext.getNeedsNextStageHash().isEmpty()) {
                if (StringTools.safeEquals(lastRetryHash, globalContext.getNeedsNextStageHash())) {
                    ++retrySameHashCount;
                    if (retrySameHashCount == 4) {
                        logger.error("Needs to retry to proceed to next stage, but even after retry, it is not progressing. Not retrying");
                    } else {
                        retry = true;
                        logger.info("Needs to retry to proceed to next stage. Retrying in 30 seconds");
                        ThreadTools.sleep(30000);
                    }
                } else {
                    retrySameHashCount = 0;
                    lastRetryHash = currentContext.getNeedsNextStageHash();
                    retry = true;
                    logger.info("Needs to retry to proceed to next stage. Retrying in 15 seconds");
                    ThreadTools.sleep(15000);
                }
            }

            globalContext.setNeedsNextStageHash(currentContext.getNeedsNextStageHash());
            globalContext.getModifications().addAll(currentContext.getModifications());

        }

        // Show summary of modifications
        System.out.println("---[ Summary of modifications ]---");
        globalContext.getModifications().forEach(it -> System.out.println(it));

        if (!globalContext.getNeedsNextStageHash().isEmpty()) {
            System.out.println("\nISSUE: Could not complete due to missing dependency");
        }

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

        logger.info("Getting Dns Zones");
        config.setAzureDnsZones(cloudAzureService.dnsZoneList().stream()
                .map(it -> new AzureDnsZoneManageConfiguration()
                        .setResource(it)
                        .setConfig(new DnsConfig()
                                .setStartEmpty(true)
                                .setConfigs(Collections.singletonList(new DnsEntryConfig()
                                                .setConflictResolution(ConflictResolution.APPEND)
                                                .setRawDnsEntries(cloudAzureService.dnsZoneEntryListIgnoreNs(it))
                                        )
                                )
                        )
                )
                .collect(Collectors.toList())
        );

        logger.info("Getting storage accounts");
        config.setAzureStorageAccounts(cloudAzureService.storageAccountList());

        logger.info("Getting web applications");
        config.setAzureWebapps(cloudAzureService.webappList().stream()
                .map(it -> new AzureWebAppManageConfiguration()
                        .setResource(it)
                )
                .collect(Collectors.toList())
        );

        logger.info("Export to {}", file);
        var json = JsonTools.prettyPrintWithoutNulls(cleanup(config));
        FileTools.writeFile(json, file);
    }

    private Object cleanup(ManageConfiguration config) {
        var cloned = JsonTools.clone(config, Map.class);
        cleanup(cloned);
        return cloned;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
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
