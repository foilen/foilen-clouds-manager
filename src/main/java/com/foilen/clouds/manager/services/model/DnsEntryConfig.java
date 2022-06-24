package com.foilen.clouds.manager.services.model;

import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;

public class DnsEntryConfig extends AbstractBasics {

    private ConflictResolution conflictResolution = ConflictResolution.APPEND;
    private List<RawDnsEntry> rawDnsEntries;

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
}
