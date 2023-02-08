/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DockerHubTagsResponse extends AbstractBasics {

    private List<DockerHubTag> results;

    public List<DockerHubTag> getResults() {
        return results;
    }

    public void setResults(List<DockerHubTag> results) {
        this.results = results;
    }

}
