/*
 * Copyright (C) 2017-2024 Dominic Heutelbeck (dominic@heutelbeck.com)
 *
 * SPDX-License-Identifier: Apache-2.0
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.sapl.server;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@Theme(value = "sapl", variant = Lumo.DARK)
public class SaplServerCeApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        context = SpringApplication.run(SaplServerCeApplication.class, args);
    }

    private static ConfigurableApplicationContext context;

    public static void restart() {
        UI.getCurrent().getPage().setLocation("/");
        VaadinSession.getCurrent().getSession().invalidate();
        ApplicationArguments args   = context.getBean(ApplicationArguments.class);
        Thread               thread = new Thread(() -> {
                                        context.close();
                                        context = SpringApplication.run(SaplServerCeApplication.class,
                                                args.getSourceArgs());
                                    });

        thread.setDaemon(false);
        thread.start();
    }
}
