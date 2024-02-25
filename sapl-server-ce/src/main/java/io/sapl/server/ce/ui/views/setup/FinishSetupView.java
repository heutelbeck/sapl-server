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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.SaplServerCeApplication;
import io.sapl.server.ce.model.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.model.setup.ApplicationConfigService;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("Finish Setup")
@Route(value = FinishSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class FinishSetupView extends VerticalLayout {

    public static final String  ROUTE                  = "/setup/finish";
    private static final String THEME_BADGEERRORPILL   = "badge error pill";
    private static final String THEME_BADGESUCCESSPILL = "badge success pill";
    private static final String PADDING_XS             = "var(--lumo-space-xs";

    @Autowired
    private transient ApplicationConfigService applicationConfigService;

    @PostConstruct
    private void init() {
        add(getLayout());
    }

    public Component getLayout() {
        Button restart = new Button("Restart Server CE");

        restart.addClickListener(e -> SaplServerCeApplication.restart());
        if (!applicationConfigService.getDbmsConfig().isSaved()
                || !applicationConfigService.getAdminUserConfig().isSaved()
                || applicationConfigService.getAt("server/address", "").toString().isEmpty()
                || applicationConfigService.getAt("server/port", "").toString().isEmpty()
                || applicationConfigService.getAt("spring.rsocket.server/port", "").toString().isEmpty()) {
            restart.setEnabled(false);
        }

        Div  adminStateView = new Div();
        Icon adminStateIcon;
        if (!applicationConfigService.getAdminUserConfig().isSaved()) {
            adminStateIcon = VaadinIcon.CLOSE.create();
            adminStateIcon.getElement().getThemeList().add(THEME_BADGEERRORPILL);
            adminStateIcon.getStyle().setPadding(PADDING_XS);
        } else {
            adminStateIcon = VaadinIcon.CHECK_CIRCLE.create();
            adminStateIcon.getElement().getThemeList().add(THEME_BADGESUCCESSPILL);
            adminStateIcon.getStyle().setPadding(PADDING_XS);
        }
        adminStateView.add(new Text("Admin user setup finished "), adminStateIcon);

        Div  dbmsStateView = new Div();
        Icon dbmsStateIcon;
        if (!applicationConfigService.getDbmsConfig().isSaved()) {
            dbmsStateIcon = VaadinIcon.CLOSE.create();
            dbmsStateIcon.getElement().getThemeList().add(THEME_BADGEERRORPILL);
            dbmsStateIcon.getStyle().setPadding(PADDING_XS);
        } else {
            dbmsStateIcon = VaadinIcon.CHECK_CIRCLE.create();
            dbmsStateIcon.getElement().getThemeList().add(THEME_BADGESUCCESSPILL);
            dbmsStateIcon.getStyle().setPadding(PADDING_XS);
        }
        dbmsStateView.add(new Text("Database setup finished "), dbmsStateIcon);

        Div  httpStateView = new Div();
        Icon httpStateIcon;
        if (applicationConfigService.getAt("server/address", "").toString().isEmpty()
                || applicationConfigService.getAt("server/port", "").toString().isEmpty()) {
            httpStateIcon = VaadinIcon.CLOSE.create();
            httpStateIcon.getElement().getThemeList().add(THEME_BADGEERRORPILL);
            httpStateIcon.getStyle().setPadding(PADDING_XS);
        } else {
            httpStateIcon = VaadinIcon.CHECK_CIRCLE.create();
            httpStateIcon.getElement().getThemeList().add(THEME_BADGESUCCESSPILL);
            httpStateIcon.getStyle().setPadding(PADDING_XS);
        }
        httpStateView.add(new Text("HTTP endpoint setup finished "), httpStateIcon);

        Div  rsocketStateView = new Div();
        Icon rsocketStateIcon;
        if (applicationConfigService.getAt("spring.rsocket.server/port", "").toString().isEmpty()) {
            rsocketStateIcon = VaadinIcon.CLOSE.create();
            rsocketStateIcon.getElement().getThemeList().add(THEME_BADGEERRORPILL);
            rsocketStateIcon.getStyle().setPadding(PADDING_XS);
        } else {
            rsocketStateIcon = VaadinIcon.CHECK_CIRCLE.create();
            rsocketStateIcon.getElement().getThemeList().add(THEME_BADGESUCCESSPILL);
            rsocketStateIcon.getStyle().setPadding(PADDING_XS);
        }
        rsocketStateView.add(new Text("RSocket endpoint setup finished "), rsocketStateIcon);

        VerticalLayout stateLayout = new VerticalLayout();
        stateLayout.setSpacing(false);
        stateLayout.getThemeList().add("spacing-l");
        stateLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        stateLayout.add(dbmsStateView);
        stateLayout.add(adminStateView);
        stateLayout.add(httpStateView);
        stateLayout.add(getTlsDisabledWarning("Http", !getTlsEnableState("server/ssl/enabled")));
        stateLayout.add(rsocketStateView);
        stateLayout.add(getTlsDisabledWarning("RSocket", !getTlsEnableState("spring.rsocket.server/ssl/enabled")));

        var hInfo = new H2(
                "The following settings must be adjusted and saved before the application can be restarted and used.");

        FormLayout adminUserLayout = new FormLayout(hInfo, stateLayout, restart);
        adminUserLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("490px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP));
        adminUserLayout.setColspan(restart, 2);
        adminUserLayout.setColspan(hInfo, 2);
        adminUserLayout.setColspan(stateLayout, 2);

        return adminUserLayout;
    }

    private Span getTlsDisabledWarning(String protocol, boolean visible) {
        String warning            = "Note: Do not use the option \"Disable TLS\" for " + protocol + " in production.\n"
                + "This option may open the server to malicious probing and exfiltration attempts through "
                + "the authorization endpoints, potentially resulting in unauthorized access to your "
                + "organization's data, depending on your policies.";
        Span   tlsDisabledWarning = new Span(warning);
        tlsDisabledWarning.getStyle().set("color", "var(--lumo-error-text-color)");
        tlsDisabledWarning.setVisible(visible);

        return tlsDisabledWarning;
    }

    private boolean getTlsEnableState(String path) {
        var result = applicationConfigService.getAt(path, false);
        if (result == null)
            return false;

        if (!(result instanceof Boolean))
            return false;

        return (Boolean) result;
    }
}
