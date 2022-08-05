/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.azureclient;

import com.azure.resourcemanager.appservice.models.CustomHostnameDnsRecordType;
import com.foilen.clouds.manager.ManageUnrecoverableException;
import com.foilen.clouds.manager.azureclient.model.Error;
import com.foilen.clouds.manager.azureclient.model.*;
import com.foilen.clouds.manager.services.AzureUtils;
import com.foilen.clouds.manager.services.CloudAzureService;
import com.foilen.clouds.manager.services.model.AzureWebApp;
import com.foilen.clouds.manager.services.model.AzureWebAppMountStorage;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import com.foilen.smalltools.tools.StringTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Client to use the Azure REST Service with resources not usable via the Azure Resource Manager library.
 * <p>
 * Official documentation: https://docs.microsoft.com/en-us/rest/api/azure/
 */
@Component
public class AzureCustomClient extends AbstractBasics {

    private static final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private CloudAzureService cloudAzureService;

    public void applicationServiceCertificateCreate(AzureWebApp azureWebApp, String domainName, String hostname) {
        var request = new AzureApplicationServiceCertificateRequest()
                .setLocation(azureWebApp.getRegionId())
                .setProperties(new AzureApplicationServiceCertificateRequestProperties()
                        .setCanonicalName(hostname)
                        .setServerFarmId(azureWebApp.getAppServicePlanId())
                );

        var idDetails = AzureUtils.getAzIdDetails(azureWebApp.getId());

        var response = put(Error.class,
                "2021-02-01",
                "https://management.azure.com/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Web/certificates/{name}",
                Map.of(
                        "subscriptionId", idDetails.getSubscriptionId(),
                        "resourceGroupName", idDetails.getResourceGroupName(),
                        "name", hostname
                ),
                request);
        if (response == null) {
            // Created
            return;
        }
        if (response.getCode() != null) {
            logger.error("Could not create certificate: {}", JsonTools.compactPrint(response));
            throw new ManageUnrecoverableException(response.getMessage());
        }
    }

    public AzureWebSite website(String id) {
        var details = AzureUtils.getAzIdDetails(id);
        return website(details.getSubscriptionId(), details.getResourceGroupName(), details.getName());
    }

    public AzureWebSite website(String subscriptionId, String resourceGroupName, String websiteName) {
        return get(AzureWebSite.class,
                "2021-02-01",
                "https://management.azure.com/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Web/sites/{websiteName}",
                Map.of(
                        "subscriptionId", subscriptionId,
                        "resourceGroupName", resourceGroupName,
                        "websiteName", websiteName
                ));
    }

    public AzureWebSiteConfigsResponse websiteConfig(String id) {
        var details = AzureUtils.getAzIdDetails(id);
        return websiteConfig(details.getSubscriptionId(), details.getResourceGroupName(), details.getName());
    }

    public AzureWebSiteConfigsResponse websiteConfig(String subscriptionId, String resourceGroupName, String websiteName) {
        return get(AzureWebSiteConfigsResponse.class,
                "2021-02-01",
                "https://management.azure.com/subscriptions/{subscriptionId}/resourceGroups/{resourceGroupName}/providers/Microsoft.Web/sites/{websiteName}/config",
                Map.of(
                        "subscriptionId", subscriptionId,
                        "resourceGroupName", resourceGroupName,
                        "websiteName", websiteName
                ));
    }

    public BaseApiResponseWithError websiteMountStorageUpdate(String websiteId, String accountResourceGroupName, Map<String, AzureWebAppMountStorage> mountStorages) {
        AzureWebSiteConfigPropertiesStorageAccountRequest request = new AzureWebSiteConfigPropertiesStorageAccountRequest();
        mountStorages.forEach((mountName, mountDetails) -> {
            request.getProperties().put(mountName, new AzureWebSiteConfigPropertiesStorageAccountRequestProperties()
                    .setAccountName(mountDetails.getAccountName())
                    .setShareName(mountDetails.getShareName())
                    .setMountPath(mountDetails.getMountPath())
                    .setAccessKey(cloudAzureService.storageAccountKey(accountResourceGroupName, mountDetails.getAccountName()))
            );
        });

        BaseApiResponseWithError response = put(BaseApiResponseWithError.class,
                "2021-02-01",
                "https://management.azure.com/{websiteId}/config/azurestorageaccounts",
                Map.of(
                        "websiteId", websiteId
                ),
                request);
        if (!response.isSuccess()) {
            logger.error("Could not update storage mount: {}", JsonTools.compactPrint(response));
            throw new ManageUnrecoverableException(response.getError().getMessage());
        }
        return response;
    }

    private <R extends BaseApiResponseWithError> R get(Class<R> responseType, String apiVersion, String url, Map<String, Object> uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cloudAzureService.getTokenManagement());
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(url)
                    .uriVariables(uriVariables)
                    .queryParam("api-version", apiVersion)
                    .toUriString();
            return restTemplate.exchange(RequestEntity.get(new URI(uri)).headers(headers).build(), responseType)
                    .getBody();
        } catch (HttpClientErrorException e) {
            return JsonTools.readFromString(e.getResponseBodyAsString(), responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <R> R put(Class<R> responseType, String apiVersion, String url, Map<String, Object> uriVariables, Object data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(cloudAzureService.getTokenManagement());
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(url)
                    .uriVariables(uriVariables)
                    .queryParam("api-version", apiVersion)
                    .toUriString();
            return restTemplate.exchange(RequestEntity.put(new URI(uri)).headers(headers).body(data), responseType)
                    .getBody();
        } catch (HttpClientErrorException e) {
            return JsonTools.readFromString(e.getResponseBodyAsString(), responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
