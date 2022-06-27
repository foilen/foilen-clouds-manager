/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2022 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager.services;

import com.foilen.clouds.manager.CliException;
import com.foilen.smalltools.restapi.model.AbstractApiBaseWithError;
import com.foilen.smalltools.restapi.model.ApiError;
import com.foilen.smalltools.restapi.model.FormResult;
import com.foilen.smalltools.tools.CollectionsTools;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Map;

@Component
public class ExceptionService {

    @Autowired
    private DisplayService displayService;

    public void displayResult(AbstractApiBaseWithError formResult, String context) {
        if (formResult.isSuccess()) {
            displayService.display("[SUCCESS] " + context);
        } else {
            displayResultError(formResult, context);
        }
    }

    public void displayResult(FormResult formResult, String context) {
        if (formResult.isSuccess()) {
            displayService.display("[SUCCESS] " + context);
        } else {
            displayResultError(formResult, context);
        }
    }

    public void displayResultAndThrow(AbstractApiBaseWithError formResult, String context) {
        displayResult(formResult, context);
        if (!formResult.isSuccess()) {
            displayResultError(formResult, context);
            throw new CliException();
        }
    }

    public void displayResultAndThrow(FormResult formResult, String context) {
        displayResult(formResult, context);
        if (!formResult.isSuccess()) {
            throw new CliException();
        }
    }

    private void displayResultError(AbstractApiBaseWithError formResult, String context) {
        displayService.display("[ERROR] " + context);

        if (formResult == null) {
            displayService.display("\tGot a null response");
            return;
        }
        ApiError error = formResult.getError();
        if (error != null) {
            displayService.display("\t" + error.getTimestamp() + " " + error.getUniqueId() + " : " + error.getMessage());
        }

    }

    private void displayResultError(FormResult formResult, String context) {
        displayService.display("[ERROR] " + context);

        ApiError error = formResult.getError();
        if (error != null) {
            displayService.display("\t" + error.getTimestamp() + " " + error.getUniqueId() + " : " + error.getMessage());
        }

        if (!CollectionsTools.isNullOrEmpty(formResult.getGlobalErrors())) {
            formResult.getGlobalErrors().forEach(it -> displayService.display("\t[GLOBAL] " + it));
        }

        if (!formResult.getValidationErrorsByField().isEmpty()) {
            formResult.getValidationErrorsByField().entrySet().stream() //
                    .sorted(Map.Entry.comparingByKey()) //
                    .forEach(entry -> entry.getValue().stream().sorted().forEach(it -> displayService.display("\t[" + entry.getKey() + "] " + it)));
        }
    }

    public void throwOnFailure(AbstractApiBaseWithError formResult, String context) {
        if (formResult == null || !formResult.isSuccess()) {
            displayResultError(formResult, context);
            throw new CliException();
        }
    }

}
