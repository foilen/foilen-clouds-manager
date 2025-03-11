/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.clouds.manager.services.CloudInMemoryService;
import com.foilen.clouds.manager.services.LetsEncryptService;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.DnsZone;
import com.foilen.clouds.manager.services.model.InMemorySecretStore;
import com.foilen.clouds.manager.services.model.SecretStore;
import com.foilen.smalltools.tools.DirectoryTools;
import com.foilen.smalltools.tools.FileTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.Map;
import java.util.Optional;

@ShellComponent
public class LetsEncryptCommands {

    @Autowired
    private CloudAzureService cloudAzureService;
    @Autowired
    private CloudInMemoryService cloudInMemoryService;
    @Autowired
    private LetsEncryptService letsEncryptService;

    @ShellMethod("Update the Let's Encrypt in local files")
    public void letsEncryptFile(
            @ShellOption String domain,
            @ShellOption String contactEmail,
            @ShellOption String azureDnsZoneId,
            @ShellOption String outDirectory,
            @ShellOption(defaultValue = "false") boolean staging
    ) {

        String secretNamespace = domain + (staging ? "-staging" : "");

        DirectoryTools.createPath(outDirectory);

        Optional<DnsZone> dnsZone = cloudAzureService.dnsFindById(azureDnsZoneId);
        if (dnsZone.isEmpty()) {
            System.out.println("Unknown Dns Zone");
            return;
        }

        var secretStore = new InMemorySecretStore();
        secretStore.setName("letsencrypt");

        // Add existing
        var all = cloudInMemoryService.keyVaultSecretGetAll(secretStore);
        addIfFileExists(outDirectory + "/account-keypair.pem", all, "account|keypairPem");
        addIfFileExists(outDirectory + "/caCertificate.pem", all, secretNamespace + "|caCertificatePem");
        addIfFileExists(outDirectory + "/certificate.pem", all, secretNamespace + "|certificatePem");
        addIfFileExists(outDirectory + "/privateKey.pem", all, secretNamespace + "|privateKeyPem");
        addIfFileExists(outDirectory + "/publicKey.pem", all, secretNamespace + "|publicKeyPem");
        addIfFileExists(outDirectory + "/pfxPassword.txt", all, secretNamespace + "|pfxPassword");

        letsEncryptService.update(domain, dnsZone.get(), secretStore, Optional.empty(), staging, contactEmail);

        // Save
        all = cloudInMemoryService.keyVaultSecretGetAll(secretStore);
        FileTools.writeFile(all.get("account|keypairPem"), outDirectory + "/account-keypair.pem");
        FileTools.writeFile(all.get(secretNamespace + "|caCertificatePem"), outDirectory + "/caCertificate.pem");
        FileTools.writeFile(all.get(secretNamespace + "|certificatePem"), outDirectory + "/certificate.pem");
        FileTools.writeFile(all.get(secretNamespace + "|privateKeyPem"), outDirectory + "/privateKey.pem");
        FileTools.writeFile(all.get(secretNamespace + "|publicKeyPem"), outDirectory + "/publicKey.pem");
        FileTools.writeFile(all.get(secretNamespace + "|pfxPassword"), outDirectory + "/pfxPassword.txt");

    }

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

    private void addIfFileExists(String filePath, Map<String, String> all, String key) {
        if (FileTools.exists(filePath)) {
            all.put(key, FileTools.getFileAsString(filePath));
        }
    }

}
