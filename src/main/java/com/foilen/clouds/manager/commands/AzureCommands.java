/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.clouds.manager.services.ResourcesBucketService;
import com.foilen.clouds.manager.services.model.AzureKeyVault;
import com.foilen.clouds.manager.services.model.SecretStore;
import com.foilen.smalltools.tools.StringTools;

@ShellComponent
public class AzureCommands {

    @Autowired
    private CloudAzureService cloudAzureService;
    @Autowired
    private ResourcesBucketService resourcesBucketService;

    @ShellMethod("Create an Azure key vault")
    public void azureKeyVaulCreate(//
            @ShellOption String keyVaultName, //
            @ShellOption String regionName, //
            @ShellOption String resourceGroup //
    ) {

        cloudAzureService.keyVaultCreate(keyVaultName, regionName, resourceGroup);

    }

    @ShellMethod("List secrets in Azure key vault")
    public void azureKeyVaulSecretList(//
            @ShellOption String keyVaultName, //
            @ShellOption(defaultValue = "false") boolean showValues //
    ) {

        AzureKeyVault azureKeyVault = findKeyVaultByNameOrFail(keyVaultName);

        System.out.println("---[ Secrets in keyvault " + keyVaultName + " ]---");
        cloudAzureService.keyVaultSecretList(azureKeyVault).streamByPage() //
                .forEach(secrets -> {
                    secrets.getElements().forEach(secret -> {
                        if (showValues) {
                            System.out.println(secret.name() + " : " + secret.getValue());
                        } else {
                            System.out.println(secret.name());
                        }
                    });
                });

    }

    @ShellMethod("Set a secret in Azure key vault")
    public void azureKeyVaulSecretSet(//
            @ShellOption String keyVaultName, //
            @ShellOption String secretName, //
            @ShellOption String value //
    ) {

        AzureKeyVault azureKeyVault = findKeyVaultByNameOrFail(keyVaultName);

        cloudAzureService.keyVaultSecretSetAsTextOrFail(azureKeyVault, secretName, value);

    }

    private AzureKeyVault findKeyVaultByNameOrFail(String keyVaultName) {
        Optional<SecretStore> secretStore = resourcesBucketService.getAzureResourcesBucket().getSecretStoresByGroup().values().stream() //
                .flatMap(it -> it.stream()) //
                .filter(it -> StringTools.safeEquals(keyVaultName, it.getName())) //
                .findAny();

        if (secretStore.isEmpty()) {
            System.out.println("Unknown key vault");
            throw new CliException("Unknown key vault");
        }
        return (AzureKeyVault) secretStore.get();
    }

}
