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

package io.sapl.server.ce.ui.views.setup;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.SaplServerCeApplication;
import io.sapl.server.ce.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.setup.ApplicationYamlHandler;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

import java.util.HashSet;
import java.util.Set;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("Finish Setup")
@Route(value = FinishSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class FinishSetupView extends VerticalLayout {

    public static final String ROUTE = "/setup/finish";

    @Autowired
    private ApplicationYamlHandler applicationYamlHandler;

    private Set<String> adminFinished = new HashSet<>();
    private Set<String> dbmsFinished  = new HashSet<>();

    @PostConstruct
    private void init() {
        add(getLayout());
    }

    public Component getLayout() {
        Button restart = new Button("Restart Server CE");

        restart.addClickListener(e -> SaplServerCeApplication.restart());
        if (applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty() || applicationYamlHandler
                .getAt("io.sapl/server/accesscontrol/admin-username", "").toString().isEmpty()) {
            restart.setEnabled(false);
        }

        adminFinished.add("Admin user setup finished");
        CheckboxGroup<String> adminUserFinished = new CheckboxGroup<>();
        adminUserFinished.setItems(adminFinished);
        adminUserFinished.getStyle().set("--vaadin-input-field-border-width", "1px");
        adminUserFinished.setEnabled(false);
        if (!applicationYamlHandler.getAt("io.sapl/server/accesscontrol/admin-username", "").toString().isEmpty())
            adminUserFinished.setValue(adminFinished);
        add(adminUserFinished);

        dbmsFinished.add("Database setup finished");
        CheckboxGroup<String> dbms = new CheckboxGroup<>();
        dbms.setItems(dbmsFinished);
        dbms.getStyle().set("--vaadin-input-field-border-width", "1px");
        dbms.setEnabled(false);
        if (!applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty())
            dbms.setValue(dbmsFinished);
        add(dbms);

        var hInfo = new H2(
                "The following settings must be adjusted and saved before the application can be restarted and used.");

        FormLayout adminUserLayout = new FormLayout(hInfo, adminUserFinished, dbms, restart);
        adminUserLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));

        adminUserLayout.setColspan(restart, 2);
        adminUserLayout.setColspan(hInfo, 2);

        return adminUserLayout;
    }
}
