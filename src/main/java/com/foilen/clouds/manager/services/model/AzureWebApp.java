/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services.model;

import com.azure.resourcemanager.appservice.fluent.models.SiteConfigInner;
import com.azure.resourcemanager.appservice.models.AppSetting;
import com.foilen.clouds.manager.azureclient.model.AzureWebSiteConfig;
import com.foilen.clouds.manager.services.model.manageconfig.Action;
import com.foilen.clouds.manager.services.model.manageconfig.Modification;
import com.foilen.smalltools.listscomparator.ListComparatorHandler;
import com.foilen.smalltools.listscomparator.ListsComparator;
import com.foilen.smalltools.tools.StringTools;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AzureWebApp extends CommonResource implements WebApp, HasResourceGroup {

    private String resourceGroup;
    private String regionId;
    private String name;

    private String appServicePlanId;
    private String publicDockerHubImage;
    private Boolean alwaysOn;
    private Boolean httpsOnly;
    private boolean webSocketsEnabled;
    private Map<String, String> appSettings = new TreeMap<>();
    private Map<String, AzureWebAppMountStorage> mountStorages = new TreeMap<>();
    private Map<String, AzureWebAppCustomHostname> customHostnames = new TreeMap<>();

    public AzureWebApp() {
        super(CloudProvider.AZURE);
    }

    public static AzureWebApp from(com.azure.resourcemanager.appservice.models.WebApp webApp, AzureWebSiteConfig webSiteConfig, Set<String> hostnamesWithCertificates) {
        AzureWebApp item = new AzureWebApp();
        item.setId(webApp.id());
        item.name = webApp.name();
        item.resourceGroup = webApp.resourceGroupName();
        item.regionId = webApp.region().name();

        item.httpsOnly = webApp.httpsOnly();
        item.appServicePlanId = webApp.appServicePlanId();
        item.webSocketsEnabled = webApp.webSocketsEnabled();
        var innerModel = webApp.innerModel();
        item.appSettings = webApp.getAppSettings().values().stream().collect(Collectors.toMap(AppSetting::key, AppSetting::value));
        if (innerModel != null) {
            SiteConfigInner siteConfig = innerModel.siteConfig();
            if (siteConfig != null) {
                item.alwaysOn = siteConfig.alwaysOn();
            }
        }
        item.publicDockerHubImage = getPublicDockerHubImage(webApp);

        webSiteConfig.getProperties().getAzureStorageAccounts().forEach((mountName, mountValue) -> item.mountStorages.put(mountName, new AzureWebAppMountStorage()
                .setMountPath(mountValue.getMountPath())
                .setAccountName(mountValue.getAccountName())
                .setShareName(mountValue.getShareName())
        ));

        webApp.getHostnameBindings().forEach((hostname, binding) -> {
            if (!hostname.endsWith(".azurewebsites.net")) {
                AzureWebAppCustomHostname customHostname = new AzureWebAppCustomHostname();
                customHostname.setCreateCertificate(hostnamesWithCertificates.contains(hostname));
                item.customHostnames.put(hostname, customHostname);
            }
        });

        return item;
    }

    private static String getPublicDockerHubImage(com.azure.resourcemanager.appservice.models.WebApp webApp) {
        var innerModel = webApp.innerModel();
        if (innerModel != null) {
            SiteConfigInner siteConfig = innerModel.siteConfig();
            if (siteConfig != null) {
                String[] linuxFxVersion = siteConfig.linuxFxVersion().split("\\|");
                if (linuxFxVersion.length == 2) {
                    return linuxFxVersion[1];
                }
            }
        }
        return null;
    }

    public List<String> differences(AzureWebApp current) {
        return differences(current, null);
    }

    public List<String> differences(AzureWebApp current, List<Function<com.azure.resourcemanager.appservice.models.WebApp, List<Modification>>> updateActions) {
        var differences = new ArrayList<String>();
        different(differences, "Web App", name, "resourceGroup", resourceGroup, current.resourceGroup);
        different(differences, "Web App", name, "regionId", regionId, current.regionId);
        different(differences, "Web App", name, "name", name, current.name);
        different(differences, "Web App", name, "appServicePlanId", appServicePlanId, current.appServicePlanId);
        different(differences, "Web App", name, "publicDockerHubImage", publicDockerHubImage, current.publicDockerHubImage, () -> {
            updateActions.add(webApp -> {
                var currentValue = getPublicDockerHubImage(webApp);
                if (StringTools.safeEquals(currentValue, publicDockerHubImage)) {
                    return Collections.emptyList();
                }

                webApp.update()
                        .withPublicDockerHubImage(publicDockerHubImage)
                        .apply();
                return Arrays.asList(new Modification().setResourceType("Web App").setResourceName(name).setAction(Action.UPDATE).setDetails("publicDockerHubImage").setFromValue(currentValue).setToValue(publicDockerHubImage));
            });
        });
        different(differences, "Web App", name, "alwaysOn", alwaysOn, current.alwaysOn);
        different(differences, "Web App", name, "httpsOnly", httpsOnly, current.httpsOnly);
        different(differences, "Web App", name, "webSocketsEnabled", webSocketsEnabled, current.webSocketsEnabled);
        different(differences, "Web App", name, "appSettings", appSettings, current.appSettings, () -> {
            updateActions.add(webApp -> {
                var currentAppSettings = webApp.getAppSettings().values().stream().collect(Collectors.toMap(AppSetting::key, AppSetting::value));
                if (currentAppSettings.equals(appSettings)) {
                    return Collections.emptyList();
                }

                var update = webApp.update();

                List<Modification> modifications = new ArrayList<>();

                ListsComparator.compareStreams(
                        currentAppSettings.entrySet().stream().sorted(Map.Entry.comparingByKey()),
                        appSettings.entrySet().stream().sorted(Map.Entry.comparingByKey()),
                        (a, b) -> a.getKey().compareTo(b.getKey()),
                        new ListComparatorHandler<>() {
                            @Override
                            public void both(Map.Entry<String, String> current, Map.Entry<String, String> desired) {
                                if (!StringTools.safeEquals(current.getValue(), desired.getValue())) {
                                    update.withAppSetting(desired.getKey(), desired.getValue());
                                    modifications.add(new Modification().setResourceType("Web App").setResourceName(name).setAction(Action.UPDATE).setDetails("appSettings|" + current.getKey()).setFromValue(current.getValue()).setToValue(desired.getValue()));
                                }
                            }

                            @Override
                            public void leftOnly(Map.Entry<String, String> current) {
                                update.withoutAppSetting(current.getKey());
                                modifications.add(new Modification().setResourceType("Web App").setResourceName(name).setAction(Action.REMOVE).setDetails("appSettings|" + current.getKey() + " => " + current.getValue()));
                            }

                            @Override
                            public void rightOnly(Map.Entry<String, String> desired) {
                                update.withAppSetting(desired.getKey(), desired.getValue());
                                modifications.add(new Modification().setResourceType("Web App").setResourceName(name).setAction(Action.ADD).setDetails("appSettings|" + desired.getKey() + " => " + desired.getValue()));
                            }
                        }
                );

                update.apply();

                return modifications;
            });
        });
        return differences;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getRegionId() {
        return regionId;
    }

    public void setRegionId(String regionId) {
        this.regionId = regionId;
    }

    @Override
    public String getResourceGroup() {
        return resourceGroup;
    }

    public void setResourceGroup(String resourceGroup) {
        this.resourceGroup = resourceGroup;
    }

    public Boolean isHttpsOnly() {
        return httpsOnly;
    }

    public void setHttpsOnly(Boolean httpsOnly) {
        this.httpsOnly = httpsOnly;
    }

    public String getAppServicePlanId() {
        return appServicePlanId;
    }

    public void setAppServicePlanId(String appServicePlanId) {
        this.appServicePlanId = appServicePlanId;
    }


    public Boolean getAlwaysOn() {
        return alwaysOn;
    }

    public void setAlwaysOn(Boolean alwaysOn) {
        this.alwaysOn = alwaysOn;
    }

    public String getPublicDockerHubImage() {
        return publicDockerHubImage;
    }

    public void setPublicDockerHubImage(String publicDockerHubImage) {
        this.publicDockerHubImage = publicDockerHubImage;
    }

    public boolean isWebSocketsEnabled() {
        return webSocketsEnabled;
    }

    public boolean getWebSocketsEnabled() {
        return webSocketsEnabled;
    }

    public void setWebSocketsEnabled(boolean webSocketsEnabled) {
        this.webSocketsEnabled = webSocketsEnabled;
    }

    public Map<String, String> getAppSettings() {
        return appSettings;
    }

    public void setAppSettings(Map<String, String> appSettings) {
        this.appSettings = appSettings;
    }

    public Boolean getHttpsOnly() {
        return httpsOnly;
    }

    public Map<String, AzureWebAppMountStorage> getMountStorages() {
        return mountStorages;
    }

    public void setMountStorages(Map<String, AzureWebAppMountStorage> mountStorages) {
        this.mountStorages = mountStorages;
    }

    public Map<String, AzureWebAppCustomHostname> getCustomHostnames() {
        return customHostnames;
    }

    public void setCustomHostnames(Map<String, AzureWebAppCustomHostname> customHostnames) {
        this.customHostnames = customHostnames;
    }
}
