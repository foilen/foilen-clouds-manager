/*
    Foilen Clouds Manager
    https://github.com/foilen/foilen-clouds-manager
    Copyright (c) 2021-2025 Foilen (https://foilen.com)

    The MIT License
    http://opensource.org/licenses/MIT

 */
package com.foilen.clouds.manager;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@Order(InteractiveShellApplicationRunner.PRECEDENCE - 100)
public class NonInteractiveApplicationRunner implements ApplicationRunner {

    private final Shell shell;
    private final ConfigurableEnvironment environment;

    public NonInteractiveApplicationRunner(Shell shell, ConfigurableEnvironment environment) {
        this.shell = shell;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments applicationArguments) {
        if (applicationArguments.getSourceArgs().length != 0) {
            InteractiveShellApplicationRunner.disable(environment);
            var command = String.join(" ", applicationArguments.getSourceArgs());
            Object result = shell.evaluate(() -> command);
            if (result != null) {
                System.out.println(result);
            }
            shell.evaluate(() -> "exit");
        }
    }
}