/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.model.DockerHubTag;
import com.foilen.clouds.manager.model.DockerHubTagsResponse;
import com.foilen.clouds.manager.model.OnlineFileDetails;
import com.foilen.smalltools.tools.AbstractBasics;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.Optional;

@Component
public class DockerHubService extends AbstractBasics {

    private static final RestTemplate restTemplate = new RestTemplate();

    public OnlineFileDetails getLatestVersionDockerHub(String imageName) {
        DockerHubTagsResponse tags = restTemplate.getForObject("https://hub.docker.com/v2/repositories/{imageName}/tags/", DockerHubTagsResponse.class,
                Collections.singletonMap("imageName", imageName));

        Optional<DockerHubTag> tag = tags.getResults().stream() //
                .filter(it -> !"latest".equals(it.getName())) //
                .findFirst();

        if (tag.isPresent()) {
            return new OnlineFileDetails() //
                    .setVersion(tag.get().getName());
        }

        return null;
    }

}
