/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.foilen.smalltools.restapi.model.AbstractApiBaseWithAdditionalProperties;

public class BaseApiResponseWithError extends AbstractApiBaseWithAdditionalProperties {

    private String id;
    private String message;
    @JsonProperty("request_id")
    private String requestId;

    public boolean isSuccess() {
        return message == null;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

}
