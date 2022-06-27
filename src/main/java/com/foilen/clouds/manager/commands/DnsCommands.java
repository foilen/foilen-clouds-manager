/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.commands;

import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.CloudService;
import com.foilen.clouds.manager.services.DisplayService;
import com.foilen.smalltools.reflection.ReflectionTools;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import org.xbill.DNS.*;

import java.lang.reflect.Field;
import java.util.*;

@ShellComponent
public class DnsCommands extends AbstractBasics {

    @Autowired
    private CloudService cloudService;
    @Autowired
    private DisplayService displayService;

    private void addSubDomains(Collection<String> hostnames, String hostname, String... subs) {
        for (String sub : subs) {
            hostnames.add(sub + "." + hostname);
        }
    }

    @ShellMethod("Look the current DNS entries for a hostname with common sub-domains")
    public void dnsQuery(
            String hostname,
            @ShellOption(defaultValue = ShellOption.NULL) String moreSubDomains,
            @ShellOption(defaultValue = ShellOption.NULL) String usingDnsServer
    ) {

        List<RawDnsEntry> dnsEntries = listRawDnsEntries(hostname, moreSubDomains, usingDnsServer);
        System.out.println("\n\n---[" + hostname + "]---");
        dnsEntries.stream().sorted().distinct()
                .forEach(it -> System.out.println(JsonTools.compactPrintWithoutNulls(it)));

    }

    private List<RawDnsEntry> listRawDnsEntries(String hostname, String moreSubDomains, String usingDnsServer) {
        // Hostnames to check
        Deque<String> hostnames = new LinkedList<>();
        hostnames.add(hostname);
        addSubDomains(hostnames, hostname,
                "ns1", "ns2", "ns3", "ns4", // DNS
                "beta", "dev", "pre", "www", "w3", // Web
                "_imap._tcp", "_imaps._tcp", "_submission._tcp", "_pop._tcp", "_pop3._tcp", "autodiscover", "email", "imap", "mail", "mx", "pop", "pop3", "smtp", // Emails
                "default._domainkey", "_amazonses", "mandrill._domainkey", "s1._domainkey", "s2._domainkey", // Mail keys
                "_caldavs._tcp", // CalDav
                "cpanel", "whm", // CPanel
                "_sip._tls", "_sipfederationtls._tcp", "sip", // SIP
                "_jabber._tcp", "_ldap._tcp", "_xmpp-client._tcp", "enterpriseenrollment", "enterpriseregistration", "ftp", "login", "lyncdiscover", "lyncdiscoverinternal", "gitlab", "phpmyadmin",
                "sso", "shop", "unifi", "unms", "webdisk", "webmail", // Common apps
                "_acme-challenge", // Let's Encrypt
                "asuid" // Azure
        );
        if (moreSubDomains != null) {
            addSubDomains(hostnames, hostname, moreSubDomains.split(","));
        }

        // Get the list of DNS Types
        Map<Integer, String> typeById = new HashMap<>();
        for (Field field : ReflectionTools.allFields(Type.class)) {
            try {
                typeById.put(field.getInt(null), field.getName());
            } catch (Exception ignored) {
            }
        }

        // Check
        Set<String> visited = new HashSet<>();
        List<RawDnsEntry> dnsEntries = new ArrayList<>();
        while (!hostnames.isEmpty()) {
            String nextHostname = hostnames.pop();
            if (!visited.add(nextHostname)) {
                continue;
            }
            logger.info("Checking {}", nextHostname);

            for (int typeToLookup : Arrays.asList(Type.ANY, Type.MX)) {

                try {
                    Lookup lookup = new Lookup(nextHostname, typeToLookup);

                    // Use a specific name server
                    if (!Strings.isNullOrEmpty(usingDnsServer)) {
                        SimpleResolver resolver = new SimpleResolver(usingDnsServer);
                        lookup.setResolver(resolver);
                    }

                    // Get the records
                    Record[] records = lookup.run();
                    if (records == null) {
                        continue;
                    }
                    for (Record record : records) {

                        String typeName = typeById.get(record.getType());
                        RawDnsEntry dnsEntry = new RawDnsEntry() //
                                .setName(record.getName().toString(true)) //
                                .setType(typeName) //
                                .setTtl(record.getTTL()) //
                                ;
                        switch (record.getType()) {
                            case Type.A:
                                dnsEntry.setDetails(((ARecord) record).getAddress().getHostAddress());
                                dnsEntries.add(dnsEntry);
                                break;
                            case Type.AAAA:
                                dnsEntry.setDetails(((AAAARecord) record).getAddress().getHostAddress());
                                dnsEntries.add(dnsEntry);
                                break;
                            case Type.CNAME:
                                dnsEntry.setDetails(((CNAMERecord) record).getTarget().toString(true));
                                dnsEntries.add(dnsEntry);
                                if (dnsEntry.getDetails().endsWith(hostname)) {
                                    hostnames.add(dnsEntry.getDetails());
                                }
                                break;
                            case Type.MX:
                                MXRecord mxRecord = (MXRecord) record;
                                dnsEntry.setDetails(mxRecord.getTarget().toString(true));
                                dnsEntry.setPriority(mxRecord.getPriority());
                                dnsEntries.add(dnsEntry);
                                if (dnsEntry.getDetails().endsWith(hostname)) {
                                    hostnames.add(dnsEntry.getDetails());
                                }
                                break;
                            case Type.NS:
                                dnsEntry.setDetails(((NSRecord) record).getTarget().toString(true));
                                dnsEntries.add(dnsEntry);
                                break;
                            case Type.TXT:
                                ((TXTRecord) record).getStrings().forEach(it -> {
                                    dnsEntry.setDetails(it);
                                    dnsEntries.add(JsonTools.clone(dnsEntry));
                                });
                                break;
                            case Type.SOA:
                                break;
                            case Type.SRV:
                                SRVRecord srvRecord = (SRVRecord) record;
                                dnsEntry.setDetails(srvRecord.getTarget().toString(true));
                                dnsEntry.setPriority(srvRecord.getPriority());
                                dnsEntry.setWeight(srvRecord.getWeight());
                                dnsEntry.setPort(srvRecord.getPort());
                                dnsEntries.add(dnsEntry);
                                if (dnsEntry.getDetails().endsWith(hostname)) {
                                    hostnames.add(dnsEntry.getDetails());
                                }
                                break;
                            default:
                                logger.error("Unknown type {} for {}", typeName, nextHostname);
                        }

                    }
                } catch (Exception e) {
                    logger.error("Problem checking {}", nextHostname, e);
                }
            }
        }

        Collections.sort(dnsEntries);
        return dnsEntries;
    }

}
