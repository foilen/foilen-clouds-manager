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

import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.smalltools.tools.AbstractBasics;

@ShellComponent
public class DiscoverCommands extends AbstractBasics {

    @Autowired
    private CloudAzureService cloudAzureService;

    @ShellMethod("Check all resources available in your Microsoft Azure account")
    public void discoverAzure( //
    ) {
        cloudAzureService.discoverAll();

    }

}
