/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

public class ManageUnrecoverableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ManageUnrecoverableException() {
        super();
    }

    public ManageUnrecoverableException(String message) {
        super(message);
    }

    public ManageUnrecoverableException(String message, Throwable cause) {
        super(message, cause);
    }

}
