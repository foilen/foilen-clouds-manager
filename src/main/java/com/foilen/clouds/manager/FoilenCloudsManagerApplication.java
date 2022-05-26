/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2021 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.foilen.clouds.manager.services.ResourcesBucketDao;

@SpringBootApplication
public class FoilenCloudsManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoilenCloudsManagerApplication.class, args);
    }

    @Bean
    public ResourcesBucketDao azureResourcesBucketDao() {
        return new ResourcesBucketDao("_dao/azureResourcesBucket.yaml");
    }

}