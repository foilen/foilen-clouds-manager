/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.foilen.smalltools.restapi.model.AbstractApiBase;

import java.util.HashMap;
import java.util.Map;

public class BaseApiResponse extends AbstractApiBase {

    private Map<String, Object> additionalProperties = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> additionalProperties() {
        return additionalProperties;
    }

    @JsonAnySetter
    public void additionalProperties(String name, Object value) {
        additionalProperties.put(name, value);
    }

}
