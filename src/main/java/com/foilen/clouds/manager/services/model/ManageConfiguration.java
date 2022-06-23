/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.foilen.smalltools.restapi.model.AbstractApiBase;

import java.util.ArrayList;
import java.util.List;

public class ManageConfiguration {

    private List<AzureResourceGroup> azureResourceGroups = new ArrayList<>();
    private List<AzureKeyVault> azureKeyVaults = new ArrayList<>();
    private List<AzureApplicationServicePlan> azureApplicationServicePlans = new ArrayList<>();
    private List<AzureMariadbManageConfiguration> azureMariadbs = new ArrayList<>();
    private List<AzureDnsZoneManageConfiguration> azureDnsZones = new ArrayList<>();

    public List<AzureResourceGroup> getAzureResourceGroups() {
        return azureResourceGroups;
    }

    public void setAzureResourceGroups(List<AzureResourceGroup> azureResourceGroups) {
        this.azureResourceGroups = azureResourceGroups;
    }

    public List<AzureKeyVault> getAzureKeyVaults() {
        return azureKeyVaults;
    }

    public void setAzureKeyVaults(List<AzureKeyVault> azureKeyVaults) {
        this.azureKeyVaults = azureKeyVaults;
    }

    public List<AzureApplicationServicePlan> getAzureApplicationServicePlans() {
        return azureApplicationServicePlans;
    }

    public void setAzureApplicationServicePlans(List<AzureApplicationServicePlan> azureApplicationServicePlans) {
        this.azureApplicationServicePlans = azureApplicationServicePlans;
    }

    public List<AzureMariadbManageConfiguration> getAzureMariadbs() {
        return azureMariadbs;
    }

    public void setAzureMariadbs(List<AzureMariadbManageConfiguration> azureMariadbs) {
        this.azureMariadbs = azureMariadbs;
    }

    public List<AzureDnsZoneManageConfiguration> getAzureDnsZones() {
        return azureDnsZones;
    }

    public void setAzureDnsZones(List<AzureDnsZoneManageConfiguration> azureDnsZones) {
        this.azureDnsZones = azureDnsZones;
    }
}
