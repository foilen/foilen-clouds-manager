/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2023 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.digitaloceanclient;

import com.foilen.clouds.manager.commands.model.RawDnsEntry;
import com.foilen.clouds.manager.digitaloceanclient.model.*;
import com.foilen.clouds.manager.services.DisabledException;
import com.foilen.smalltools.tools.AbstractBasics;
import com.foilen.smalltools.tools.JsonTools;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

/**
 * Client to use the Digital Ocean REST Service.
 * <p>
 * Official documentation: https://docs.digitalocean.com/reference/api/api-reference/
 */
@Component
public class DigitalOceanCustomClient extends AbstractBasics {

    private static final RestTemplate restTemplate = new RestTemplate();

    @Value("${DIGITALOCEAN_TOKEN:#{null}}")
    private String digitalOceanToken;

    private void isEnabled() {
        if (digitalOceanToken == null) {
            throw new DisabledException("Digital Ocean is not enabled");
        }
    }

    public DigitalOceanDomainsResponse domainList() {
        isEnabled();
        return get(DigitalOceanDomainsResponse.class, "https://api.digitalocean.com/v2/domains", null, null);
    }

    public DigitalOceanDomainRecordsResponse domainRecordList(String domainName) {
        isEnabled();
        Map<String, String> queryParam = Map.of("per_page", "200");
        Map<String, Object> uriVariable = Map.of("domainName", domainName);
        return get(DigitalOceanDomainRecordsResponse.class, "https://api.digitalocean.com/v2/domains/{domainName}/records", uriVariable, queryParam);
    }

    public DigitalOceanDomainRecordResponse domainRecordAdd(String domainName, RawDnsEntry entry) {
        isEnabled();
        Map<String, Object> uriVariable = Map.of("domainName", domainName);
        var digitalOceanDomainRecord = new DigitalOceanDomainRecord();
        digitalOceanDomainRecord.setName(toName(entry.getName(), domainName));
        digitalOceanDomainRecord.setType(DigitalOceanDomainRecordType.valueOf(entry.getType()));
        digitalOceanDomainRecord.setData(entry.getDetails());
        digitalOceanDomainRecord.setPriority(entry.getPriority());
        digitalOceanDomainRecord.setWeight(entry.getWeight());
        digitalOceanDomainRecord.setPort(entry.getPort());
        digitalOceanDomainRecord.setTtl(entry.getTtl());
        switch (digitalOceanDomainRecord.getType()) {
            case CNAME:
            case MX:
                digitalOceanDomainRecord.setData(digitalOceanDomainRecord.getData() + ".");
                break;
        }
        return post(DigitalOceanDomainRecordResponse.class, "https://api.digitalocean.com/v2/domains/{domainName}/records", uriVariable, digitalOceanDomainRecord);
    }

    public BaseApiResponseWithError domainRecordDelete(String domainName, String id) {
        isEnabled();
        Map<String, Object> uriVariable = Map.of(
                "domainName", domainName,
                "id", id);
        return delete(BaseApiResponseWithError.class, "https://api.digitalocean.com/v2/domains/{domainName}/records/{id}", uriVariable);
    }

    private String toName(String fullName, String domainName) {
        if (fullName.endsWith("." + domainName)) {
            return fullName.substring(0, fullName.length() - domainName.length() - 1);
        }
        return "@";
    }

    private <R extends BaseApiResponseWithError> R get(Class<R> responseType, String url, Map<String, Object> uriVariables, Map<String, String> queryParam) {

        if (uriVariables == null) {
            uriVariables = Map.of();
        }

        MultiValueMap<String, String> queryParamMultimap = null;
        if (queryParam != null) {
            queryParamMultimap = new LinkedMultiValueMap<>();
            queryParam.forEach(queryParamMultimap::add);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(digitalOceanToken);
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(url)
                    .uriVariables(uriVariables)
                    .queryParams(queryParamMultimap)
                    .toUriString();
            return restTemplate.exchange(RequestEntity.get(new URI(uri)).headers(headers).build(), responseType)
                    .getBody();
        } catch (HttpClientErrorException e) {
            return JsonTools.readFromString(e.getResponseBodyAsString(), responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <R> R delete(Class<R> responseType, String url, Map<String, Object> uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(digitalOceanToken);
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(url)
                    .uriVariables(uriVariables)
                    .toUriString();
            R body = restTemplate.exchange(RequestEntity.delete(new URI(uri)).headers(headers).build(), responseType)
                    .getBody();
            if (body == null) {
                body = responseType.getDeclaredConstructor().newInstance();
            }
            return body;
        } catch (HttpClientErrorException e) {
            return JsonTools.readFromString(e.getResponseBodyAsString(), responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <R> R post(Class<R> responseType, String url, Map<String, Object> uriVariables, Object data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(digitalOceanToken);
        try {
            var uri = UriComponentsBuilder.fromHttpUrl(url)
                    .uriVariables(uriVariables)
                    .toUriString();
            return restTemplate.exchange(RequestEntity.post(new URI(uri)).headers(headers).body(data), responseType)
                    .getBody();
        } catch (HttpClientErrorException e) {
            return JsonTools.readFromString(e.getResponseBodyAsString(), responseType);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
