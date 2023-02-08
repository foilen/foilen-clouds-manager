/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.clouds.manager.commands.model.ClearDnsEntry;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;

public class DnsEntryConfig extends AbstractBasics {

    private ConflictResolution conflictResolution = ConflictResolution.APPEND;
    private List<RawDnsEntry> rawDnsEntries;
    private List<ClearDnsEntry> clearDnsEntries;
    private List<FoilenCloudDnsEntries> foilenCloudDnsEntries;

    private List<AzureUidDnsEntry> azureUidDnsEntry;
    private List<AzureCustomDomainDnsEntry> azureCustomDomainDnsEntry;

    public ConflictResolution getConflictResolution() {
        return conflictResolution;
    }

    public DnsEntryConfig setConflictResolution(ConflictResolution conflictResolution) {
        this.conflictResolution = conflictResolution;
        return this;
    }

    public List<RawDnsEntry> getRawDnsEntries() {
        return rawDnsEntries;
    }

    public DnsEntryConfig setRawDnsEntries(List<RawDnsEntry> rawDnsEntries) {
        this.rawDnsEntries = rawDnsEntries;
        return this;
    }

    public List<ClearDnsEntry> getClearDnsEntries() {
        return clearDnsEntries;
    }

    public DnsEntryConfig setClearDnsEntries(List<ClearDnsEntry> clearDnsEntries) {
        this.clearDnsEntries = clearDnsEntries;
        return this;
    }

    public List<FoilenCloudDnsEntries> getFoilenCloudDnsEntries() {
        return foilenCloudDnsEntries;
    }

    public DnsEntryConfig setFoilenCloudDnsEntries(List<FoilenCloudDnsEntries> foilenCloudDnsEntries) {
        this.foilenCloudDnsEntries = foilenCloudDnsEntries;
        return this;
    }

    public List<AzureUidDnsEntry> getAzureUidDnsEntry() {
        return azureUidDnsEntry;
    }

    public DnsEntryConfig setAzureUidDnsEntry(List<AzureUidDnsEntry> azureUidDnsEntry) {
        this.azureUidDnsEntry = azureUidDnsEntry;
        return this;
    }

    public List<AzureCustomDomainDnsEntry> getAzureCustomDomainDnsEntry() {
        return azureCustomDomainDnsEntry;
    }

    public DnsEntryConfig setAzureCustomDomainDnsEntry(List<AzureCustomDomainDnsEntry> azureCustomDomainDnsEntry) {
        this.azureCustomDomainDnsEntry = azureCustomDomainDnsEntry;
        return this;
    }
}
