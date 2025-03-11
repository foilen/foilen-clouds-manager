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
import com.foilen.clouds.manager.services.CloudDigitalOceanService;
import com.foilen.clouds.manager.services.model.DigitalOceanDnsZone;
import com.foilen.smalltools.tools.InternetTools;
import com.foilen.smalltools.tools.StringTools;
import com.foilen.smalltools.tools.ThreadTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.ArrayList;
import java.util.List;

@ShellComponent
public class DigitalOceanCommands {

    @Autowired
    private CloudDigitalOceanService cloudDigitalOceanService;

    @ShellMethod("Set the IP of a DNS Zone entry")
    public void digitalOceanDnsZoneEntryUpdate(
            @ShellOption String dnsZoneName,
            @ShellOption String hostname,
            @ShellOption(defaultValue = ShellOption.NULL, help = "The IP to set or it will get the public IP of the current machine") String ip,
            @ShellOption(defaultValue = "false") boolean keepAlive
    ) {

        if (keepAlive && ip != null) {
            throw new CliException("Cannot set an IP and keep alive at the same time");
        }

        System.out.println("DNS Zone Name: " + dnsZoneName);
        System.out.println("Hostname: " + hostname);

        DigitalOceanDnsZone dnsZone = new DigitalOceanDnsZone();
        dnsZone.setName(dnsZoneName);
        RawDnsEntry previousDnsEntry = cloudDigitalOceanService.dnsZoneEntryList(dnsZone).stream()
                .filter(it -> it.getName().equals(hostname))
                .findFirst()
                .orElse(new RawDnsEntry());
        System.out.println("Current IP in DNS Zone: " + previousDnsEntry.getDetails());

        boolean firstPass = true;
        while (firstPass || keepAlive) {
            firstPass = false;

            if (ip == null || keepAlive) {
                ip = InternetTools.getPublicIp();
            }
            System.out.println("IP: " + ip);

            if (!StringTools.safeEquals(ip, previousDnsEntry.getDetails())) {
                System.out.println("Updating the DNS Zone entry");
                List<RawDnsEntry> toDeleteEntries = new ArrayList<>();
                if (previousDnsEntry.get_id() != null) {
                    toDeleteEntries.add(previousDnsEntry);
                }
                List<RawDnsEntry> newDnsEntries = cloudDigitalOceanService.dnsUpdateEntries(dnsZone, toDeleteEntries, List.of(new RawDnsEntry()
                                .setName(hostname)
                                .setType("A")
                                .setTtl(300)
                                .setDetails(ip)
                        )
                );
                previousDnsEntry = newDnsEntries.get(0);
            }

            if (keepAlive) {
                ThreadTools.sleep(2 * 60 * 1000);
            }
        }
    }

}
