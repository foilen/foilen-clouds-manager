/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient.model;

import com.foilen.smalltools.tools.CollectionsTools;

import java.util.List;

public class AzureListResource<T extends BaseApiResponse> extends BaseApiResponseWithError {

    private String id;
    private List<T> value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<T> getValue() {
        return value;
    }

    public T getSingleValue() {
        if (CollectionsTools.isNullOrEmpty(value)) {
            return null;
        }
        return value.get(0);
    }

    public void setValue(List<T> value) {
        this.value = value;
    }
}
