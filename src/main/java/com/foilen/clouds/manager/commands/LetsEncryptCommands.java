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

import com.foilen.clouds.manager.services.LetsEncryptService;
import com.foilen.clouds.manager.services.ResourcesBucketService;
import com.foilen.clouds.manager.services.model.DomainConfiguration;

@ShellComponent
public class LetsEncryptCommands {

    @Autowired
    private LetsEncryptService letsEncryptService;
    @Autowired
    private ResourcesBucketService resourcesBucketService;

    @ShellMethod("Update the Let's Encrypt certificate")
    public void letsEncryptUpdate(//
            @ShellOption String domain, //
            @ShellOption String contactEmail, //
            @ShellOption(defaultValue = "false") boolean staging //
    ) {

        DomainConfiguration configuration = resourcesBucketService.getDomainConfig(domain);

        if (configuration == null) {
            System.out.println("Unknown domain");
            return;
        }

        letsEncryptService.update(configuration, staging, contactEmail);

    }

}
