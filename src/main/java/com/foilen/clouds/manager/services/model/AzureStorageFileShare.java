/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.storage.file.share.models.ShareItem;

import java.util.ArrayList;
import java.util.List;

public class AzureStorageFileShare extends CommonResource implements Comparable<AzureStorageFileShare>, HasName {

    private String name;

    private int quotaInGB;
    private String accessTier;

    private boolean smbEnabled;
    private boolean nfsEnabled;

    public AzureStorageFileShare() {
        super(CloudProvider.AZURE);
    }

    public static AzureStorageFileShare from(ShareItem shareItem) {
        var item = new AzureStorageFileShare();
        item.name = shareItem.getName();
        var properties = shareItem.getProperties();
        if (properties != null) {
            item.quotaInGB = properties.getQuota();
            item.accessTier = properties.getAccessTier();
            var protocols = properties.getProtocols();
            if (protocols != null) {
                item.smbEnabled = protocols.isSmbEnabled();
                item.nfsEnabled = protocols.isNfsEnabled();
            }
        }
        return item;
    }

    public List<String> differences(AzureStorageFileShare current) {
        var differences = new ArrayList<String>();
        different(differences, "Storage File Service", name, "name", name, current.name);
        different(differences, "Storage File Service", name, "quotaInGB", quotaInGB, current.quotaInGB);
        different(differences, "Storage File Service", name, "accessTier", accessTier, current.accessTier);
        different(differences, "Storage File Service", name, "smbEnabled", smbEnabled, current.smbEnabled);
        different(differences, "Storage File Service", name, "nfsEnabled", nfsEnabled, current.nfsEnabled);
        return differences;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAccessTier() {
        return accessTier;
    }

    public void setAccessTier(String accessTier) {
        this.accessTier = accessTier;
    }

    public boolean isSmbEnabled() {
        return smbEnabled;
    }

    public void setSmbEnabled(boolean smbEnabled) {
        this.smbEnabled = smbEnabled;
    }

    public boolean isNfsEnabled() {
        return nfsEnabled;
    }

    public void setNfsEnabled(boolean nfsEnabled) {
        this.nfsEnabled = nfsEnabled;
    }

    public int getQuotaInGB() {
        return quotaInGB;
    }

    public void setQuotaInGB(int quotaInGB) {
        this.quotaInGB = quotaInGB;
    }

    @Override
    public int compareTo(AzureStorageFileShare o) {
        return name.compareTo(o.name);
    }
}
