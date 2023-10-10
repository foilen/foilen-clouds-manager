/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import java.util.Objects;

public class InMemorySecretStore extends CommonResource implements SecretStore {

    private String name;

    public InMemorySecretStore() {
        super(CloudProvider.IN_MEMORY);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof InMemorySecretStore) {
            return Objects.equals(getId(), ((InMemorySecretStore) obj).getId());
        }
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getProvider(), getId());
    }

}
