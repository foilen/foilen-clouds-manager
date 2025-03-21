/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

import com.foilen.clouds.manager.digitaloceanclient.model.BaseApiResponseWithError;
import com.foilen.smalltools.restapi.model.ApiError;

public class CliException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CliException() {
        super();
    }

    public CliException(ApiError error) {
        super(error.getTimestamp() + " " + error.getUniqueId() + " " + error.getMessage());
    }

    public CliException(String message) {
        super(message);
    }

    public CliException(String message, Throwable cause) {
        super(message, cause);
    }

    public CliException(String message, BaseApiResponseWithError result) {
        super(message + ": " + result.getMessage());
    }

}
