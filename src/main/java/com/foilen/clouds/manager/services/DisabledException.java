/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

public class DisabledException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DisabledException(String message) {
        super(message);
    }

}
