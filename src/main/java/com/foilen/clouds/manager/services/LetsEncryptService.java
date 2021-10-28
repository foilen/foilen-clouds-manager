/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.foilen.clouds.manager.CliException;
import com.foilen.clouds.manager.services.model.DomainConfiguration;
import com.foilen.clouds.manager.services.model.SecretStore;
import com.foilen.clouds.manager.services.model.WebApp;
import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class LetsEncryptService extends AbstractBasics {

    @Autowired
    private CloudService cloudService;
    @Autowired
    private ResourcesBucketService resourcesBucketService;

    public void update(DomainConfiguration configuration) {

        logger.info("Will update certificate for domain {}", configuration.getDomainName());

        if (configuration.getDnsZones().isEmpty()) {
            throw new CliException("There is no automated way to manage the DNS Zone");
        }

        // Find a secret store
        SecretStore secretStore = cloudService.findSecretStoreOrFail(resourcesBucketService.getAllResourcesBucket(), configuration, "letsenc-");

        // TODO + LE process (use/create account ; get challenge ; update DNS ; get cert)

        for (WebApp httpsWebApp : configuration.getHttpsWebApp()) {
            logger.info("Deploy certificate on {}", httpsWebApp);
            // TODO + LE deploy
        }

    }

}
