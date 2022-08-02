/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model.manageconfig;

import com.foilen.smalltools.hash.HashSha256;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class ManageContext {

    private List<String> modifications = new ArrayList<>();

    private String needsNextStageHash = "";

    private void addModification(String resourceType, String resourceName, Action action, String details) {
        modifications.add(resourceType + " (" + resourceName + ") " + action + " " + details);
    }

    public void addModificationAdd(String resourceType, String resourceName) {
        addModification(resourceType, resourceName, Action.ADD, "");
    }

    public void addModificationAdd(String resourceType, String resourceName, String details) {
        addModification(resourceType, resourceName, Action.ADD, details);
    }

    public void addModificationUpdate(String resourceType, String resourceName, String details, String fromValue, String toValue) {
        addModification(resourceType, resourceName, Action.UPDATE, details + ": " + fromValue + " -> " + toValue);
    }

    public void addModificationRemove(String resourceType, String resourceName) {
        addModification(resourceType, resourceName, Action.REMOVE, "");
    }

    public void addModificationRemove(String resourceType, String resourceName, String details) {
        addModification(resourceType, resourceName, Action.REMOVE, details);
    }

    public List<String> getModifications() {
        return modifications;
    }

    public void setModifications(List<String> modifications) {
        this.modifications = modifications;
    }

    public String getNeedsNextStageHash() {
        return needsNextStageHash;
    }

    public void setNeedsNextStageHash(String needsNextStageHash) {
        this.needsNextStageHash = needsNextStageHash;
    }

    public void needsNextStage(String... failureDetails) {
        needsNextStageHash = HashSha256.hashString(needsNextStageHash + Joiner.on("|").join(failureDetails));
    }
}
