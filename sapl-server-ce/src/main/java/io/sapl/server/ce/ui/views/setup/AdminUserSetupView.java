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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.SaplServerCeApplication;
import io.sapl.server.ce.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.config.ApplicationYamlHandler;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("Admin User Setup")
@Route(value = AdminUserSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class AdminUserSetupView extends VerticalLayout {

    public static final String ROUTE = "/setup/admin";

    private ApplicationYamlHandler applicationYamlHandler;
    private final TextField        username      = new TextField("Username");
    private final PasswordField    pwd           = new PasswordField("Password");
    private final PasswordField    pwdRepeat     = new PasswordField("Repeat Password");
    private final Button           pwdSaveConfig = new Button("Save Admin-User Settings");
    private final Button           restart       = new Button("Restart Server CE");

    private final Span adminUserErrorMessage = new Span();
    private boolean    enablePasswordCheck   = false;

    @Autowired
    public AdminUserSetupView(ApplicationYamlHandler appYH) {
        this.applicationYamlHandler = appYH;
    }

    @PostConstruct
    private void init() {
        add(getLayout());

    }

    public Component getLayout() {

        pwdSaveConfig.setEnabled(false);
        pwdSaveConfig.addClickListener(e -> {
            PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            applicationYamlHandler.setAt("io.sapl/server/accesscontrol/admin-username", username.getValue());
            applicationYamlHandler.setAt("io.sapl/server/accesscontrol/encoded-admin-password",
                    encoder.encode(pwd.getValue()));
            applicationYamlHandler.writeYamlFile();
            ConfirmUtils.inform("saved", "Username and password successfully saved");
            if (!applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty()) {
                restart.setEnabled(true);
            }
        });

        restart.addClickListener(e -> SaplServerCeApplication.restart());
        if (applicationYamlHandler.getAt("spring/datasource/url", "").toString().isEmpty()) {
            restart.setEnabled(false);
        }

        adminUserErrorMessage.setVisible(false);
        adminUserErrorMessage.getStyle().set("color", "var(--lumo-error-text-color)");

        username.addValueChangeListener(e -> validateAdminUser());
        pwd.addValueChangeListener(e -> validateAdminUser());
        pwdRepeat.addValueChangeListener(e -> {
            enablePasswordCheck = true;
            validateAdminUser();
        });
        enablePasswordCheck = false;

        FormLayout adminUserLayout = new FormLayout(username, pwd, pwdRepeat, adminUserErrorMessage, pwdSaveConfig,
                restart);
        adminUserLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        adminUserLayout.setColspan(username, 2);
        adminUserLayout.setColspan(adminUserErrorMessage, 2);
        adminUserLayout.setColspan(pwdSaveConfig, 2);
        adminUserLayout.setColspan(restart, 2);

        return adminUserLayout;
    }

    private void validateAdminUser() {
        List<String> errors = new ArrayList<>();
        if (username.getValue().isBlank()) {
            errors.add("Username has to be set");
        }
        if (enablePasswordCheck && !pwd.getValue().isEmpty() && !pwdRepeat.getValue().isEmpty()
                && !pwd.getValue().equals(pwdRepeat.getValue())) {
            errors.add("Passwords do not match");
        }
        if (!errors.isEmpty() && !pwd.getValue().isEmpty()) {
            adminUserErrorMessage.getElement().setProperty("innerHTML", String.join("<br />", errors));
            adminUserErrorMessage.setVisible(true);
            pwdSaveConfig.setEnabled(false);
        } else {
            adminUserErrorMessage.setVisible(false);
            pwdSaveConfig.setEnabled(true);
        }
    }

}
