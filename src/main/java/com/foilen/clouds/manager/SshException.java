/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

import java.util.Queue;

public class SshException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Queue<String> lastErrorLines;

    public SshException(String message, Queue<String> lastErrorLines) {
        super(message);
        this.lastErrorLines = lastErrorLines;
    }

    public SshException(String message, Throwable cause, Queue<String> lastErrorLines) {
        super(message, cause);
        this.lastErrorLines = lastErrorLines;
    }

    public Queue<String> getLastErrorLines() {
        return lastErrorLines;
    }

}
