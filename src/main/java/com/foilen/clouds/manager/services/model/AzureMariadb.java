/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.mariadb.models.Server;
import com.azure.resourcemanager.mariadb.models.StorageProfile;
import com.foilen.smalltools.tools.JsonTools;

import java.util.ArrayList;
import java.util.List;

public class AzureMariadb extends CommonResource implements Mariadb, HasResourceGroup {

    public static AzureMariadb from(Server server) {
        var item = new AzureMariadb();
        item.setId(server.id());
        item.name = server.name();
        if (item.getId() != null) {
            var parts = item.getId().split("/");
            if (parts.length > 3) {
                item.setResourceGroup(parts[4]);
            }
        }
        item.region = server.regionName();
        item.version = server.version().toString();
        item.sslEnforcement = server.sslEnforcement().name();
        item.minimalTlsVersion = server.minimalTlsVersion().toString();
        item.publicNetworkAccess = server.publicNetworkAccess().toString();
        item.administratorLogin = server.administratorLogin();
        item.skuName = server.sku().name();
        item.storageProfile = server.storageProfile();
        return item;
    }

    private String resourceGroup;
    private String region;
    private String name;

    private String version;
    private String sslEnforcement;
    private String minimalTlsVersion;
    private String publicNetworkAccess;
    private String administratorLogin;
    private String administratorLoginPassword;
    private String skuName;
    private StorageProfile storageProfile;

    public AzureMariadb() {
        super(CloudProvider.AZURE);
    }

    public List<String> differences(AzureMariadb current) {
        var differences = new ArrayList<String>();
        different(differences, "Mariadb", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Mariadb", name, "region", region, current.region);
        different(differences, "Mariadb", name, "version", version, current.version);
        different(differences, "Mariadb", name, "sslEnforcement", sslEnforcement, current.sslEnforcement);
        different(differences, "Mariadb", name, "minimalTlsVersion", minimalTlsVersion, current.minimalTlsVersion);
        different(differences, "Mariadb", name, "administratorLogin", administratorLogin, current.administratorLogin);
        different(differences, "Mariadb", name, "skuName", skuName, current.skuName);
        different(differences, "Mariadb", name, "storageProfile", JsonTools.compactPrintWithoutNulls(storageProfile), JsonTools.compactPrintWithoutNulls(current.storageProfile));
        return differences;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getRegion() {
        return region;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getSslEnforcement() {
        return sslEnforcement;
    }

    public void setSslEnforcement(String sslEnforcement) {
        this.sslEnforcement = sslEnforcement;
    }

    public String getAdministratorLogin() {
        return administratorLogin;
    }

    public void setAdministratorLogin(String administratorLogin) {
        this.administratorLogin = administratorLogin;
    }

    public String getSkuName() {
        return skuName;
    }

    public void setSkuName(String skuName) {
        this.skuName = skuName;
    }

    public StorageProfile getStorageProfile() {
        return storageProfile;
    }

    public void setStorageProfile(StorageProfile storageProfile) {
        this.storageProfile = storageProfile;
    }

    public String getMinimalTlsVersion() {
        return minimalTlsVersion;
    }

    public void setMinimalTlsVersion(String minimalTlsVersion) {
        this.minimalTlsVersion = minimalTlsVersion;
    }

    public String getAdministratorLoginPassword() {
        return administratorLoginPassword;
    }

    public void setAdministratorLoginPassword(String administratorLoginPassword) {
        this.administratorLoginPassword = administratorLoginPassword;
    }

    public String getPublicNetworkAccess() {
        return publicNetworkAccess;
    }

    public void setPublicNetworkAccess(String publicNetworkAccess) {
        this.publicNetworkAccess = publicNetworkAccess;
    }
}
