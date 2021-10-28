/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import org.springframework.stereotype.Component;

@Component
public class ContextService {

    private String url;

    public void useUrl(String url) {
        clear();
        this.url = url;
    }

    private void clear() {
        url = null;
    }

    public String getPrompt() {
        String prompt = "";

        if (url != null) {
            prompt += "Url: " + url;
        }

        prompt += " > ";

        return prompt;
    }

}
