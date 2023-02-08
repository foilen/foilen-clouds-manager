/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.manageconfig;

import com.foilen.smalltools.tools.AbstractBasics;

public class Modification extends AbstractBasics {

    private String resourceType;
    private String resourceName;
    private Action action;
    private String details;
    private String fromValue;
    private String toValue;

    public String getResourceType() {
        return resourceType;
    }

    public Modification setResourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    public String getResourceName() {
        return resourceName;
    }

    public Modification setResourceName(String resourceName) {
        this.resourceName = resourceName;
        return this;
    }

    public Action getAction() {
        return action;
    }

    public Modification setAction(Action action) {
        this.action = action;
        return this;
    }

    public String getDetails() {
        return details;
    }

    public Modification setDetails(String details) {
        this.details = details;
        return this;
    }

    public String getFromValue() {
        return fromValue;
    }

    public Modification setFromValue(String fromValue) {
        this.fromValue = fromValue;
        return this;
    }

    public String getToValue() {
        return toValue;
    }

    public Modification setToValue(String toValue) {
        this.toValue = toValue;
        return this;
    }
}
