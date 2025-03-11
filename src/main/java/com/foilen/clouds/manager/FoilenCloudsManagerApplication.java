/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;

@SpringBootApplication(exclude = MongoAutoConfiguration.class)
public class FoilenCloudsManagerApplication {

    public static void main(String[] args) {
        System.setProperty("spring.xml.ignore", "true");

        var app = new SpringApplication(FoilenCloudsManagerApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

}
