/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.clouds.manager.services.LetsEncryptService;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.DnsZone;
import com.foilen.clouds.manager.services.model.SecretStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Optional;

@ShellComponent
public class LetsEncryptCommands {

    @Autowired
    private CloudAzureService cloudAzureService;
    @Autowired
    private LetsEncryptService letsEncryptService;

    @ShellMethod("Update the Let's Encrypt certificate")
    public void letsEncryptUpdate(
            @ShellOption String domain,
            @ShellOption String contactEmail,
            @ShellOption String azureDnsZoneId,
            @ShellOption String azureKeyVaultId,
            @ShellOption(defaultValue = ShellOption.NULL) String azureWebappId,
            @ShellOption(defaultValue = "false") boolean staging
    ) {

        Optional<DnsZone> dnsZone = cloudAzureService.dnsFindById(azureDnsZoneId);
        if (dnsZone.isEmpty()) {
            System.out.println("Unknown Dns Zone");
            return;
        }
        Optional<SecretStore> secretStore = cloudAzureService.keyVaultFindById(azureKeyVaultId);
        if (secretStore.isEmpty()) {
            System.out.println("Unknown Secret Store");
            return;
        }
        Optional<AzureWebApp> httpsWebAppToPushCert = azureWebappId == null ? Optional.empty() : cloudAzureService.webappFindById(azureWebappId);
        letsEncryptService.update(domain, dnsZone.get(), secretStore.get(), Optional.of(httpsWebAppToPushCert.get()), staging, contactEmail);

    }

}
