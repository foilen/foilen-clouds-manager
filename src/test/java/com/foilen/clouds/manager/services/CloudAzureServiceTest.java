/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import com.foilen.clouds.manager.services.model.json.AzProfileDetails;
import com.foilen.smalltools.test.asserts.AssertTools;
import com.foilen.smalltools.tools.ResourceTools;

public class CloudAzureServiceTest {

    @Test
    public void testGetAzureProfile() throws Exception {

        String azureProfileFile = Files.createTempFile(null, null).toFile().getAbsolutePath();
        ResourceTools.copyToFile("CloudAzureServiceTest-testGetAzureProfile-profile.json", getClass(), new File(azureProfileFile));

        AzProfileDetails actual = CloudAzureService.getAzureProfile(azureProfileFile);
        AssertTools.assertJsonComparison("CloudAzureServiceTest-testGetAzureProfile-expected.json", getClass(), actual);
    }

}
