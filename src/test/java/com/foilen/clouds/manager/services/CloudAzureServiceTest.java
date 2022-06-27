/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.services.model.ConflictResolution;
import com.foilen.clouds.manager.services.model.DnsConfig;
import com.foilen.clouds.manager.services.model.DnsEntryConfig;
import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.ResourceTools;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CloudAzureServiceTest {

    @Test
    public void testComputeDnsEntries_StartEmpty() {
        var service = new CloudAzureService();
        List<RawDnsEntry> initial = Arrays.asList(
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("ABC"),
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("DEF"),

                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.1"),

                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2"),
                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.3"),

                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.4"),

                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.5").setPriority(10),
                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.6").setPriority(15)
        );

        DnsConfig config = new DnsConfig();
        config.setConfigs(Arrays.asList(
                new DnsEntryConfig()
                        .setConflictResolution(ConflictResolution.OVERWRITE)
                        .setRawDnsEntries(Arrays.asList(
                                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.10"),

                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.11"),
                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.12"),

                                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.13"),

                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ")
                        )),
                new DnsEntryConfig()
                        .setConflictResolution(ConflictResolution.APPEND)
                        .setRawDnsEntries(Arrays.asList(
                                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.20"),

                                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.24").setPriority(21),

                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.21"),
                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.22"),

                                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.23"),

                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ"),
                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("YYY")
                        ))
        ));
        config.setStartEmpty(true);

        var actual = service.computeDnsEntries("example.com", initial, config);

        List<RawDnsEntry> expected = Arrays.asList(
                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.10"),
                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.20"),

                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.24").setPriority(21),

                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.11"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.12"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.21"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.22"),

                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.13"),
                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.23"),

                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ"),
                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("YYY")
        );
        Collections.sort(expected);
        AssertTools.assertJsonComparison(expected, actual);
    }

    public void testComputeDnsEntries_NotStartEmpty() {
        var service = new CloudAzureService();
        List<RawDnsEntry> initial = Arrays.asList(
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("ABC"),
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("DEF"),

                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.1"),

                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2"),
                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.3"),

                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.4"),

                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.5").setPriority(10),
                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.6").setPriority(15)
        );

        DnsConfig config = new DnsConfig();
        config.setConfigs(Arrays.asList(
                new DnsEntryConfig()
                        .setConflictResolution(ConflictResolution.OVERWRITE)
                        .setRawDnsEntries(Arrays.asList(
                                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.10"),

                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.11"),
                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.12"),

                                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.13"),

                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ")
                        )),
                new DnsEntryConfig()
                        .setConflictResolution(ConflictResolution.APPEND)
                        .setRawDnsEntries(Arrays.asList(
                                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.20"),

                                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.24").setPriority(21),

                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.21"),
                                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.22"),

                                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.23"),

                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ"),
                                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("YYY")
                        ))
        ));
        config.setStartEmpty(false);

        var actual = service.computeDnsEntries("example.com", initial, config);

        List<RawDnsEntry> expected = Arrays.asList(
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("ABC"),
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("DEF"),

                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.10"),
                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.20"),

                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2"),
                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.3"),

                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.4"),

                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.5").setPriority(10),
                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.6").setPriority(15),
                new RawDnsEntry().setName("c.example.com").setType("MX").setDetails("127.0.0.24").setPriority(21),

                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.11"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.12"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.21"),
                new RawDnsEntry().setName("d.example.com").setType("A").setDetails("127.0.0.22"),

                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.13"),
                new RawDnsEntry().setName("e.example.com").setType("A").setDetails("127.0.0.23"),

                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("XYZ"),
                new RawDnsEntry().setName("e.example.com").setType("TXT").setDetails("YYY")
        );
        Collections.sort(expected);
        AssertTools.assertJsonComparison(expected, actual);
    }

    @Test
    public void testComputeDnsEntries_MergeTtls() {
        var service = new CloudAzureService();
        List<RawDnsEntry> initial = Arrays.asList();

        DnsConfig config = new DnsConfig();
        config.setConfigs(Arrays.asList(
                new DnsEntryConfig()
                        .setConflictResolution(ConflictResolution.OVERWRITE)
                        .setRawDnsEntries(Arrays.asList(
                                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("ABC").setTtl(20),
                                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("DEF").setTtl(100),

                                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.1").setTtl(200),

                                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2").setTtl(300),
                                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2").setTtl(500),
                                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.3"),

                                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.4"),
                                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.5")
                        ))
        ));
        config.setStartEmpty(true);

        var actual = service.computeDnsEntries("example.com", initial, config);

        List<RawDnsEntry> expected = Arrays.asList(
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("ABC").setTtl(20),
                new RawDnsEntry().setName("a.example.com").setType("TXT").setDetails("DEF").setTtl(20),

                new RawDnsEntry().setName("a.example.com").setType("A").setDetails("127.0.0.1").setTtl(200),

                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.2").setTtl(300),
                new RawDnsEntry().setName("b.example.com").setType("A").setDetails("127.0.0.3").setTtl(300),

                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.4").setTtl(172800),
                new RawDnsEntry().setName("c.example.com").setType("A").setDetails("127.0.0.5").setTtl(172800)
        );
        Collections.sort(expected);
        AssertTools.assertJsonComparison(expected, actual);
    }

    @Test
    public void testDnsSubDomain() {
        Assert.assertEquals("@", CloudAzureService.dnsSubDomain("foilen.com", "foilen.com"));
        Assert.assertEquals("abc", CloudAzureService.dnsSubDomain("foilen.com", "abc.foilen.com"));
        Assert.assertEquals("zzzzzz._domainkey", CloudAzureService.dnsSubDomain("foilen.com", "zzzzzz._domainkey.foilen.com"));
        Assert.assertEquals(null, CloudAzureService.dnsSubDomain("foilen.com", "test.another.com"));
        Assert.assertEquals(null, CloudAzureService.dnsSubDomain("foilen.com", "aaaaaa.com"));
        Assert.assertEquals(null, CloudAzureService.dnsSubDomain("foilen.com", "en.com"));
        Assert.assertEquals(null, CloudAzureService.dnsSubDomain("foilen.com", "somethingfoilen.com"));
    }

    @Test
    public void testGetAzureProfile() throws Exception {

        String azureProfileFile = Files.createTempFile(null, null).toFile().getAbsolutePath();
        ResourceTools.copyToFile("CloudAzureServiceTest-testGetAzureProfile-profile.json", getClass(), new File(azureProfileFile));

        AzProfileDetails actual = CloudAzureService.getAzureProfile(azureProfileFile);
        AssertTools.assertJsonComparison("CloudAzureServiceTest-testGetAzureProfile-expected.json", getClass(), actual);
    }

}
