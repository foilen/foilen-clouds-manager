/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.digitaloceanclient.DigitalOceanCustomClient;
import com.foilen.clouds.manager.digitaloceanclient.model.DigitalOceanDomainRecord;
import com.foilen.clouds.manager.services.model.ConflictResolution;
import com.foilen.clouds.manager.services.model.DigitalOceanDnsZone;
import com.foilen.clouds.manager.services.model.DnsConfig;
import com.foilen.clouds.manager.services.model.DnsEntryConfig;
import com.foilen.clouds.manager.services.model.manageconfig.DigitalOceanDnsZoneManageConfiguration;
import com.foilen.clouds.manager.services.model.manageconfig.ManageContext;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.AssertTools;
import com.foilen.smalltools.tools.CollectionsTools;
import com.foilen.smalltools.tools.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class CloudDigitalOceanService extends AbstractBasics {

    @Autowired
    private DigitalOceanCustomClient digitalOceanCustomClient;

    protected List<RawDnsEntry> computeDnsEntries(String domainName, List<RawDnsEntry> currentRawDnsEntries, DnsConfig desiredDnsConfig) {
        // Copy all the current entries if desired
        Map<String, Set<RawDnsEntry>> desiredEntriesByNameType = new HashMap<>();
        if (desiredDnsConfig.isStartEmpty()) {
            if (desiredDnsConfig.getStartWithDomains() != null) {
                currentRawDnsEntries.stream()
                        .filter(it -> desiredDnsConfig.getStartWithDomains().contains(it.getName()))
                        .forEach(it -> {
                            String nameType = it.getName() + "|" + it.getType();
                            CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                                    .add(it);
                        });
            }
        } else {
            currentRawDnsEntries.forEach(it -> {
                String nameType = it.getName() + "|" + it.getType();
                CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                        .add(it);
            });
        }

        // Compute the desired entries
        List<DnsEntryConfig> configs = desiredDnsConfig.getConfigs() == null ? Collections.emptyList() : desiredDnsConfig.getConfigs();
        for (var configEntries : configs) {

            // Raw entries
            if (configEntries.getRawDnsEntries() != null) {
                applyRawDnsEntriesOnMap(desiredEntriesByNameType, configEntries.getConflictResolution(), configEntries.getRawDnsEntries());
            }

            // Clear entries
            if (configEntries.getClearDnsEntries() != null) {
                configEntries.getClearDnsEntries().forEach(it -> {
                    logger.info("Clear entries of name {} and type {}", it.getName(), it.getType());
                    String nameType = it.getName() + "|" + it.getType();
                    desiredEntriesByNameType.remove(nameType);
                });
            }

            // Digital Ocean UID
            if (configEntries.getAzureUidDnsEntry() != null) {
                throw new CliException("AzureUidDnsEntry not supported yet");
            }

            // Digital Ocean Custom Domain
            if (configEntries.getAzureCustomDomainDnsEntry() != null) {
                throw new CliException("AzureCustomDomainDnsEntry not supported yet");
            }
        }

        // Update TTL for all with same name/type
        desiredEntriesByNameType.values().forEach(entries -> {
            final var ttl = entries.stream().map(RawDnsEntry::getTtl).reduce(172800L, Math::min);
            entries.forEach(entry -> entry.setTtl(ttl));
        });

        return desiredEntriesByNameType.values().stream()
                .flatMap(Set::stream)
                .filter(it -> dnsIsSubDomain(domainName, it.getName()))
                .sorted().distinct()
                .collect(Collectors.toList());

    }

    private static void applyRawDnsEntriesOnMap(Map<String, Set<RawDnsEntry>> desiredEntriesByNameType, ConflictResolution conflictResolution, List<RawDnsEntry> rawDnsEntries) {
        // Remove any existing ones if OVERWRITE
        if (conflictResolution == ConflictResolution.OVERWRITE) {
            for (RawDnsEntry it : rawDnsEntries) {
                String nameType = it.getName() + "|" + it.getType();
                desiredEntriesByNameType.remove(nameType);
            }
        }

        // Add entries
        for (RawDnsEntry it : rawDnsEntries) {
            String nameType = it.getName() + "|" + it.getType();
            CollectionsTools.getOrCreateEmptyHashSet(desiredEntriesByNameType, nameType, RawDnsEntry.class)
                    .add(it);
        }
    }

    protected static boolean dnsIsSubDomain(String baseDomainName, String fullDomainName) {
        if (fullDomainName.length() <= baseDomainName.length()) {
            return baseDomainName.equals(fullDomainName);
        } else return fullDomainName.endsWith("." + baseDomainName);
    }

    public List<DigitalOceanDnsZone> domainList() {
        logger.info("List DNS Zones");
        var result = digitalOceanCustomClient.domainList();
        if (!result.isSuccess()) {
            throw new CliException("Could not get the domains", result);
        }
        return result.getDomains().stream()
                .map(it -> {
                    var domain = new DigitalOceanDnsZone();
                    domain.setId(it.getName());
                    return domain;
                })
                .collect(Collectors.toList());
    }

    public void dnsZoneManage(ManageContext context, DigitalOceanDnsZoneManageConfiguration desired) {

        var desiredResource = desired.getResource();

        AssertTools.assertNotNull(desiredResource, "resource must be provided");
        AssertTools.assertNotNull(desiredResource.getName(), "resource.name must be provided");

        // Apply entries
        if (desired.getConfig() != null) {
            // Get what is currently present
            var currentEntries = dnsZoneEntryListIgnoreNs(desiredResource);
            var desiredEntries = computeDnsEntries(desiredResource.getName(), currentEntries, desired.getConfig());

            matchIds(currentEntries, desiredEntries);

            // Delete current that the id is not in desired
            var usedIds = desiredEntries.stream()
                    .map(RawDnsEntry::get_id)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            currentEntries.stream()
                    .filter(it -> !usedIds.contains(it.get_id()))
                    .forEach(it -> {
                        logger.info("[{}] Delete: {}", desiredResource.getName(), it);
                        var result = digitalOceanCustomClient.domainRecordDelete(desiredResource.getName(), it.get_id());
                        if (!result.isSuccess()) {
                            throw new CliException("Could not delete the domain record", result);
                        }
                        context.addModificationRemove("Digital Ocean DNS Record", desiredResource.getName(), it.toString());
                    });

            // Add desired _id = null
            desiredEntries.stream()
                    .filter(it -> it.get_id() == null)
                    .forEach(it -> {
                        logger.info("[{}] Add: {}", desiredResource.getName(), it);
                        var result = digitalOceanCustomClient.domainRecordAdd(desiredResource.getName(), it);
                        if (!result.isSuccess()) {
                            throw new CliException("Could not add the domain record", result);
                        }
                        context.addModificationAdd("Digital Ocean DNS Record", desiredResource.getName(), it.toString());
                    });

        }

    }

    private void matchIds(List<RawDnsEntry> currentEntries, List<RawDnsEntry> desiredEntries) {
        Set<String> usedIds = new HashSet<>();
        desiredEntries.forEach(desired -> {
            var matching = currentEntries.stream()
                    .filter(current -> StringTools.safeEquals(current.getName(), desired.getName())
                            && StringTools.safeEquals(current.getType(), desired.getType())
                            && StringTools.safeEquals(current.getDetails(), desired.getDetails())
                            && current.getTtl() == desired.getTtl()
                            && Objects.equals(current.getPriority(), desired.getPriority())
                            && Objects.equals(current.getPort(), desired.getPort())
                            && Objects.equals(current.getWeight(), desired.getWeight())
                    )
                    .collect(Collectors.toList());
            for (var match : matching) {
                if (match.get_id() != null) {
                    if (usedIds.add(match.get_id())) {
                        desired.set_id(match.get_id());
                        break;
                    }
                }
            }
        });
    }

    public List<RawDnsEntry> dnsZoneEntryListIgnoreNs(DigitalOceanDnsZone dnsZone) {
        var entries = dnsZoneEntryList(dnsZone);
        entries.removeIf(it -> StringTools.safeEquals(it.getType(), "NS"));
        return entries;
    }

    public List<RawDnsEntry> dnsZoneEntryList(DigitalOceanDnsZone dnsZone) {

        String domainName = dnsZone.getName();
        logger.info("Get DNS Zone {}", domainName);
        var records = digitalOceanCustomClient.domainRecordList(domainName);
        if (!records.isSuccess()) {
            throw new CliException("Could not get the domain records", records);
        }

        return records.getDomainRecords().stream()
                .map(record -> toRawDnsEntry(record, domainName))
                .filter(Objects::nonNull)
                .sorted()
                .collect(Collectors.toList());
    }

    private RawDnsEntry toRawDnsEntry(DigitalOceanDomainRecord record, String domainName) {
        RawDnsEntry rawDnsEntry = null;

        switch (record.getType()) {
            case A:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(record.getData())
                        .setTtl(record.getTtl());
                break;
            case AAAA:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(record.getData())
                        .setTtl(record.getTtl());
                break;
            case CNAME:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(toCname(record.getData(), domainName))
                        .setTtl(record.getTtl());
                break;
            case MX:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(toCname(record.getData(), domainName))
                        .setPriority(record.getPriority())
                        .setTtl(record.getTtl());
                break;
            case NS:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(record.getData())
                        .setTtl(record.getTtl());
                break;
            case SRV:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(record.getData())
                        .setPort(record.getPort())
                        .setPriority(record.getPriority())
                        .setWeight(record.getWeight())
                        .setTtl(record.getTtl());
                break;
            case TXT:
                rawDnsEntry = new RawDnsEntry()
                        .set_id(record.getId())
                        .setName(toFullDomainName(record.getName(), domainName))
                        .setType(record.getType().name())
                        .setDetails(record.getData())
                        .setTtl(record.getTtl());
                break;
        }
        return rawDnsEntry;
    }

    public List<RawDnsEntry> dnsUpdateEntries(DigitalOceanDnsZone dnsZone, List<RawDnsEntry> toDeleteEntries, List<RawDnsEntry> toAddEntries) {

        String domainName = dnsZone.getName();

        // Delete
        for (RawDnsEntry toDeleteEntry : toDeleteEntries) {
            logger.info("Delete: {}", toDeleteEntry);
            var result = digitalOceanCustomClient.domainRecordDelete(domainName, toDeleteEntry.get_id());
            if (!result.isSuccess()) {
                throw new CliException("Could not delete the domain record", result);
            }
        }

        // Add
        List<RawDnsEntry> newDnsEntries = new ArrayList<>();
        for (RawDnsEntry toAddEntry : toAddEntries) {
            logger.info("Add: {}", toAddEntry);
            var result = digitalOceanCustomClient.domainRecordAdd(domainName, toAddEntry);
            if (!result.isSuccess()) {
                throw new CliException("Could not add the domain record", result);
            }
            newDnsEntries.add(toRawDnsEntry(result.getDomainRecord(), domainName));
        }

        // Get the new list
        return newDnsEntries;
    }

    private String toCname(String name, String domainName) {
        if (StringTools.safeEquals("@", name)) {
            return domainName;
        } else {
            return name;
        }
    }

    private String toFullDomainName(String name, String domainName) {
        if (StringTools.safeEquals("@", name)) {
            return domainName;
        } else {
            return name + "." + domainName;
        }
    }
}
