/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

public class AzureWebAppMountStorage extends AbstractBasics {

    private String accountName;
    private String shareName;
    private String mountPath;

    public String getAccountName() {
        return accountName;
    }

    public AzureWebAppMountStorage setAccountName(String accountName) {
        this.accountName = accountName;
        return this;
    }

    public String getShareName() {
        return shareName;
    }

    public AzureWebAppMountStorage setShareName(String shareName) {
        this.shareName = shareName;
        return this;
    }

    public String getMountPath() {
        return mountPath;
    }

    public AzureWebAppMountStorage setMountPath(String mountPath) {
        this.mountPath = mountPath;
        return this;
    }

}
