/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import com.foilen.clouds.manager.services.DisplayService;
import com.foilen.clouds.manager.services.ResourcesBucketService;
import com.foilen.clouds.manager.services.model.ResourcesBucket;

@ShellComponent
public class ResourcesCommands {

    @Autowired
    private DisplayService displayService;
    @Autowired
    private ResourcesBucketService resourcesBucketService;

    @ShellMethod("List all known resources")
    public void resourceList() {

        ResourcesBucket resourcesBucket = resourcesBucketService.getAllResourcesBucket();

        // Secret Stores
        System.out.println("---[ Secret Stores ]---");
        resourcesBucket.getSecretStoresByGroup().forEach((group, secretStores) -> {

            System.out.println(group);
            secretStores.forEach(entry -> {
                displayService.displayResource(1, entry);
            });

        });
        System.out.println();

        // Domains
        System.out.println("---[ Domains ]---");
        resourcesBucket.getConfigurationByDomain().forEach((domain, configuration) -> {

            System.out.println(domain);

            System.out.println("\tDnsZones: ");
            configuration.getDnsZones().forEach(entry -> {
                displayService.displayResource(2, entry);
            });

            System.out.println("\tHTTP: ");
            configuration.getHttpWebApp().forEach(entry -> {
                displayService.displayResource(2, entry);
            });

            System.out.println("\tHTTPS: ");
            configuration.getHttpsWebApp().forEach(entry -> {
                displayService.displayResource(2, entry);
            });

            System.out.println();
        });
    }

    @ShellMethod("List all known domains")
    public void resourceListDomains() {
        resourcesBucketService.getAllResourcesBucket().getConfigurationByDomain().keySet().forEach(domain -> System.out.println(domain));
    }

}
