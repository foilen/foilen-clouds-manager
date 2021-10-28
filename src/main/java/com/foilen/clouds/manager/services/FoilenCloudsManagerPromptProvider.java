/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import org.jline.utils.AttributedString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

import com.foilen.smalltools.tools.AbstractBasics;

@Component
public class FoilenCloudsManagerPromptProvider extends AbstractBasics implements PromptProvider {

    @Autowired
    private ContextService contextService;

    @Override
    public AttributedString getPrompt() {
        return new AttributedString(contextService.getPrompt());
    }

}
