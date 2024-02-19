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

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.setup.ApplicationYamlHandler;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.IOException;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("Admin User Setup")
@Route(value = AdminUserSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class AdminUserSetupView extends VerticalLayout {

    public static final String ROUTE = "/setup/admin";

    @Autowired
    private ApplicationYamlHandler applicationYamlHandler;
    private static String          user              = "";
    private static String          pwd               = "";
    private static String          pwdRepeat         = "";
    private static boolean         enableSaveConfigBtn;
    private final TextField        username          = new TextField("Username");
    private final PasswordField    password          = new PasswordField("Password");
    private final PasswordField    passwordRepeat    = new PasswordField("Repeat Password");
    private final Button           pwdSaveConfig     = new Button("Save Admin-User Settings");
    private final Icon             pwdEqualCheckIcon = VaadinIcon.CHECK.create();
    private Span                   passwordStrengthText;
    private Span                   passwordEqualText;

    @PostConstruct
    private void init() {
        add(getLayout());

    }

    public Component getLayout() {
        pwdSaveConfig.setEnabled(enableSaveConfigBtn);
        pwdSaveConfig.addClickListener(e -> {
            PasswordEncoder encoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
            applicationYamlHandler.setAt("io.sapl/server/accesscontrol/admin-username", username.getValue());
            applicationYamlHandler.setAt("io.sapl/server/accesscontrol/encoded-admin-password",
                    encoder.encode(password.getValue()));
            try {
                applicationYamlHandler.saveYamlFiles();
                ConfirmUtils.inform("saved", "Username and password successfully saved");
            } catch (IOException ioe) {
                ConfirmUtils.inform("IO-Error",
                        "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                                + ioe.getMessage());
            }
        });

        username.setValue(user);
        username.addValueChangeListener(
                e -> validateAdminUser(username.getValue(), password.getValue(), passwordRepeat.getValue()));
        username.setValueChangeMode(ValueChangeMode.EAGER);
        username.setRequiredIndicatorVisible(true);
        username.setRequired(true);

        FormLayout adminUserLayout = new FormLayout(username, pwdLayout(), pwdRepeatLayout(), pwdSaveConfig);
        adminUserLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        adminUserLayout.setColspan(username, 2);
        adminUserLayout.setColspan(pwdSaveConfig, 2);

        return adminUserLayout;
    }

    private PasswordField pwdLayout() {
        Div passwordStrength = new Div();
        passwordStrengthText = new Span();
        passwordStrength.add(new Text("Password strength: "), passwordStrengthText);
        password.setHelperComponent(passwordStrength);

        add(password);
        password.setValue(pwd);
        password.setValueChangeMode(ValueChangeMode.EAGER);
        password.addValueChangeListener(e -> {
            validateAdminUser(username.getValue(), password.getValue(), passwordRepeat.getValue());
            pwdStrengthText(e.getValue());
            pwdEqualText(e.getValue(), passwordRepeat.getValue());
        });

        pwdStrengthText("");

        return password;
    }

    private PasswordField pwdRepeatLayout() {
        pwdEqualCheckIcon.setVisible(false);
        pwdEqualCheckIcon.getStyle().set("color", "var(--lumo-success-color)");
        passwordRepeat.setValue(pwdRepeat);
        passwordRepeat.setSuffixComponent(pwdEqualCheckIcon);
        passwordRepeat.setValueChangeMode(ValueChangeMode.EAGER);
        passwordRepeat.addValueChangeListener(e -> {
            validateAdminUser(username.getValue(), password.getValue(), passwordRepeat.getValue());
            pwdEqualText(password.getValue(), e.getValue());
        });

        Div passwordEqual = new Div();
        passwordEqualText = new Span();
        passwordEqual.add(new Text("Password is "), passwordEqualText);
        passwordRepeat.setHelperComponent(passwordEqual);

        add(passwordRepeat);

        pwdStrengthText("");

        return passwordRepeat;
    }

    private void pwdStrengthText(String password) {
        if (password.length() > 9) {
            passwordStrengthText.setText("strong");
            passwordStrengthText.getStyle().set("color", "var(--lumo-success-color)");
        } else if (password.length() > 5) {
            passwordStrengthText.setText("moderate");
            passwordStrengthText.getStyle().set("color", "#e7c200");
        } else {
            passwordStrengthText.setText("weak");
            passwordStrengthText.getStyle().set("color", "var(--lumo-error-color)");
        }
    }

    private void pwdEqualText(String pwd, String pwdRepeat) {
        if (pwd.equals(pwdRepeat)) {
            passwordEqualText.setText("equal");
            passwordEqualText.getStyle().set("color", "var(--lumo-success-color)");
        } else if (pwd.isBlank() || pwdRepeat.isBlank()) {
            passwordEqualText.setText("not set");
            passwordEqualText.getStyle().set("color", "#e7c200");
        } else {
            passwordEqualText.setText("not equal");
            passwordEqualText.getStyle().set("color", "var(--lumo-error-color)");
        }
    }

    private void validateAdminUser(String user, String pwd, String pwdRepeat) {
        AdminUserSetupView.user      = user;
        AdminUserSetupView.pwd       = pwd;
        AdminUserSetupView.pwdRepeat = pwdRepeat;
        pwdEqualCheckIcon.setVisible(pwd.equals(pwdRepeat));
        enableSaveConfigBtn = pwdEqualCheckIcon.isVisible() && !user.isEmpty();
        pwdSaveConfig.setEnabled(enableSaveConfigBtn);
    }

}
