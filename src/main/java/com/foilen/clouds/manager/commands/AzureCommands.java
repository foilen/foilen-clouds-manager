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
import org.springframework.shell.standard.ShellOption;

import com.foilen.clouds.manager.services.CloudAzureService;

@ShellComponent
public class AzureCommands {

    @Autowired
    private CloudAzureService cloudAzureService;

    @ShellMethod("Create an Azure key vault")
    public void azureKeyVaulCreate(//
            @ShellOption String keyVaultName, //
            @ShellOption String regionName, //
            @ShellOption String resourceGroup //
    ) {

        cloudAzureService.keyVaultCreate(keyVaultName, regionName, resourceGroup);

    }

}
