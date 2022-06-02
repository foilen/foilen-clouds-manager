/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import com.foilen.clouds.manager.services.ManageService;
import com.foilen.smalltools.tools.AbstractBasics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ManageCommands extends AbstractBasics {

    @Autowired
    private ManageService manageService;

    @ShellMethod("Add/update resources that are in file")
    public void manageResources(
            @ShellOption String file
    ) {
        manageService.manage(file);
    }

    @ShellMethod("Export resources in file")
    public void manageResourcesExport(
            @ShellOption String file
    ) {
        manageService.export(file);
    }

}
