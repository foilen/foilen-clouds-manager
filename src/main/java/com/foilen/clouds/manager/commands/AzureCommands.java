/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.clouds.manager.services.model.AzureDnsZone;
import com.foilen.clouds.manager.services.model.AzureKeyVault;
import com.foilen.smalltools.tools.InternetTools;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tools.ThreadTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.Optional;

@ShellComponent
public class AzureCommands {

    @Autowired
    private CloudAzureService cloudAzureService;

    @ShellMethod("Set the IP of a DNS Zone entry")
    public void azureDnsZoneEntryUpdate(
            @ShellOption String resourceGroupName,
            @ShellOption String dnsZoneName,
            @ShellOption String hostname,
            @ShellOption(defaultValue = ShellOption.NULL, help = "The IP to set or it will get the public IP of the current machine") String ip,
            @ShellOption(defaultValue = "false") boolean keepAlive
    ) {

        if (keepAlive && ip != null) {
            throw new CliException("Cannot set an IP and keep alive at the same time");
        }

        System.out.println("Resource Group Name: " + resourceGroupName);
        System.out.println("DNS Zone Name: " + dnsZoneName);
        System.out.println("Hostname: " + hostname);

        var dnsZone = cloudAzureService.dnsZoneFindByName(resourceGroupName, dnsZoneName).orElse(null);
        if (dnsZone == null) {
            System.out.println("Unknown DNS Zone");
            throw new CliException("Unknown DNS Zone");
        }

        String ipInAzure = cloudAzureService.dnsZoneEntryList(dnsZone).stream() //
                .filter(it -> it.getName().equals(hostname)) //
                .map(RawDnsEntry::getDetails) //
                .findFirst() //
                .orElse(null);
        System.out.println("Current Azure IP: " + ipInAzure);

        boolean firstPass = true;
        while (firstPass || keepAlive) {
            firstPass = false;

            if (ip == null || keepAlive) {
                ip = InternetTools.getPublicIp();
            }
            System.out.println("IP: " + ip);

            if (!StringTools.safeEquals(ip, ipInAzure)) {
                System.out.println("Updating the DNS Zone entry");
                cloudAzureService.dnsSetEntry(dnsZone, hostname, "A", List.of(new RawDnsEntry()
                                .setName(hostname)
                                .setType("A")
                                .setTtl(300)
                                .setDetails(ip)
                        )
                );
                ipInAzure = ip;
            }

            if (keepAlive) {
                ThreadTools.sleep(2 * 60 * 1000);
            }
        }
    }

    @ShellMethod("List the DNS Zones")
    public void azureDnsZoneList() {

        for (AzureDnsZone azureDnsZone : cloudAzureService.dnsZoneList()) {
            System.out.println(azureDnsZone.getName() + " (" + azureDnsZone.getId() + ")");
        }

    }

    @ShellMethod("List the entries in the DNS Zone")
    public void azureDnsZoneEntryList(
            @ShellOption String resourceGroupName,
            @ShellOption String dnsZoneName
    ) {

        var azureDnsZone = cloudAzureService.dnsZoneFindByName(resourceGroupName, dnsZoneName);
        if (azureDnsZone.isEmpty()) {
            System.out.println("Unknown dns zone");
            throw new CliException("Unknown dns zone");
        }

        for (var rawDnsEntry : cloudAzureService.dnsZoneEntryList(azureDnsZone.get())) {
            System.out.println(rawDnsEntry);
        }

    }

    @ShellMethod("Create an Azure key vault")
    public void azureKeyVaulCreate(
            @ShellOption String resourceGroupName,
            @ShellOption(defaultValue = ShellOption.NULL) String regionName,
            @ShellOption String keyVaultName
    ) {
        cloudAzureService.keyVaultCreate(resourceGroupName, Optional.ofNullable(regionName), keyVaultName);
    }

    @ShellMethod("List secrets in Azure key vault")
    public void azureKeyVaulSecretList(
            @ShellOption String resourceGroupName, //
            @ShellOption String keyVaultName, //
            @ShellOption(defaultValue = "false") boolean showValues //
    ) {

        AzureKeyVault azureKeyVault = findKeyVaultByNameOrFail(resourceGroupName, keyVaultName);

        System.out.println("---[ Secrets in keyvault " + keyVaultName + " ]---");
        cloudAzureService.keyVaultSecretList(azureKeyVault).streamByPage() //
                .forEach(secrets -> secrets.getElements().forEach(secret -> {
                    if (showValues) {
                        System.out.println(secret.name() + " : " + secret.getValue());
                    } else {
                        System.out.println(secret.name());
                    }
                }));

    }

    @ShellMethod("Set a secret in Azure key vault")
    public void azureKeyVaulSecretSet(
            @ShellOption String resourceGroupName,
            @ShellOption String keyVaultName,
            @ShellOption String secretName,
            @ShellOption String value
    ) {

        AzureKeyVault azureKeyVault = findKeyVaultByNameOrFail(resourceGroupName, keyVaultName);

        cloudAzureService.keyVaultSecretSetAsTextOrFail(azureKeyVault, secretName, value);

    }

    @ShellMethod("List the MariaDB databases")
    public void azureMariadbList() {

        for (var azureMariadb : cloudAzureService.mariadbList()) {
            System.out.println(azureMariadb.getName() + " (" + azureMariadb.getId() + ")");
        }

    }

    @ShellMethod("Sync local folder to target")
    public void azureStorageSyncTo(
            @ShellOption String resourceGroupName,
            @ShellOption String storageAccountName,
            @ShellOption String shareName,
            @ShellOption String sourceFolder,
            @ShellOption(defaultValue = "") String targetFolder
    ) {

        try {
            cloudAzureService.storageFileShareUpload(resourceGroupName, storageAccountName, shareName, sourceFolder, targetFolder);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private AzureKeyVault findKeyVaultByNameOrFail(String resourceGroupName, String keyVaultName) {
        Optional<AzureKeyVault> azureKeyVault = cloudAzureService.keyVaultFindByName(resourceGroupName, keyVaultName);

        if (azureKeyVault.isEmpty()) {
            System.out.println("Unknown key vault");
            throw new CliException("Unknown key vault");
        }
        return azureKeyVault.get();
    }

}
