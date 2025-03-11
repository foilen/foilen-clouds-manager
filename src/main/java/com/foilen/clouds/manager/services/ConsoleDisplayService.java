/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.smalltools.tools.AbstractBasics;
import org.springframework.stereotype.Component;

@Component
public class ConsoleDisplayService extends AbstractBasics implements DisplayService {

    @Override
    public void display(String text) {
        System.out.println(text);
    }

}
