/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.tools.AbstractBasics;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public abstract class CommonResource extends AbstractBasics implements HasCloudProvider {

    private final CloudProvider provider;

    private String id;

    public CommonResource(CloudProvider provider) {
        this.provider = provider;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public CloudProvider getProvider() {
        return provider;
    }

    protected void different(List<String> differences, String type, String name, String field, Object desired, Object current) {
        different(differences, type, name, field, desired, current, null);
    }

    protected void different(List<String> differences, String type, String name, String field, Object desired, Object current, Runnable updateRunnable) {
        if (!Objects.equals(desired, current)) {
            if (updateRunnable == null) {
                differences.add(type + " " + name + " has different " + field + ". Desired " + desired + " ; Current " + current);
            } else {
                updateRunnable.run();
            }
        }
    }

}
