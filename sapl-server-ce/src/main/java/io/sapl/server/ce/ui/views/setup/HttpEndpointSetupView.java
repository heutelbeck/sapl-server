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
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.listbox.ListBox;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.config.ApplicationYamlHandler;
import io.sapl.server.ce.ui.views.SetupLayout;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Conditional;

import java.util.ArrayList;
import java.util.List;

@AnonymousAllowed
@RequiredArgsConstructor
@PageTitle("HTTP Endpoint Configuration")
@Route(value = HttpEndpointSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class HttpEndpointSetupView extends VerticalLayout {
    public static final String ROUTE = "/setup/http";

    private ApplicationYamlHandler      applicationYamlHandler;
    private final ListBox<String>       listBox       = new ListBox<>();
    private final CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
    private final Button                tlsSaveConfig = new Button("Save HTTP Endpoint Configuration");

    private static final String TLS_V1_3_PROTOCOL          = "TLSv1.3";
    private static final String TLS_V1_3_AND_V1_2_PROTOCOL = "TLSv1.3 + TLSv1.2";

    @PostConstruct
    private void init() {
        applicationYamlHandler = new ApplicationYamlHandler();
        add(getLayout());

    }

    public Component getLayout() {
        listBox.setItems(TLS_V1_3_PROTOCOL, TLS_V1_3_AND_V1_2_PROTOCOL);
        listBox.setValue(TLS_V1_3_PROTOCOL);

        tlsSaveConfig.addClickListener(e -> writeTlsConfigToApplicationYml());

        checkboxGroup.setLabel("TLS ciphers");
        checkboxGroup.setItems(getCiphers());
        checkboxGroup.addSelectionListener(e -> checkIfAtLeastOneCipherOptionSelected());

        checkboxGroup.select("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        add(checkboxGroup);

        // TODO keystore configuration (type, path, password, alias)

        FormLayout tlsLayout = new FormLayout(listBox, checkboxGroup, tlsSaveConfig);
        tlsLayout.setColspan(listBox, 2);
        tlsLayout.setColspan(checkboxGroup, 2);
        tlsLayout.setColspan(tlsSaveConfig, 2);

        return tlsLayout;
    }

    private void checkIfAtLeastOneCipherOptionSelected() {
        tlsSaveConfig.setEnabled(!checkboxGroup.getSelectedItems().isEmpty());

        if (checkboxGroup.getSelectedItems().isEmpty()) {
            Notification notification = new Notification();
            notification.addThemeVariants(NotificationVariant.LUMO_ERROR);

            Div text = new Div(new Text("At least one cipher option must be selected"));

            Button closeButton = new Button(new Icon("lumo", "cross"));
            closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
            closeButton.setAriaLabel("Close");
            closeButton.addClickListener(event -> notification.close());

            HorizontalLayout layout = new HorizontalLayout(text, closeButton);
            layout.setAlignItems(Alignment.CENTER);

            notification.add(layout);
            notification.setPosition(Notification.Position.MIDDLE);
            notification.open();
        }
    }

    private List<String> getCiphers() {
        List<String> ciphers = new ArrayList<>();
        ciphers.add("TLS_AES_128_GCM_SHA256");
        ciphers.add("TLS_AES_256_GCM_SHA384");
        ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384");
        ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");
        ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384");
        ciphers.add("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");
        ciphers.add("TLS_DHE_RSA_WITH_AES_256_GCM_SHA384");
        ciphers.add("TLS_DHE_RSA_WITH_AES_128_GCM_SHA256");
        ciphers.add("TLS_DHE_DSS_WITH_AES_256_GCM_SHA384");
        ciphers.add("TLS_DHE_DSS_WITH_AES_128_GCM_SHA256");
        ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384");
        ciphers.add("TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256");
        ciphers.add("TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384");
        ciphers.add("TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256");
        ciphers.add("TLS_DHE_RSA_WITH_AES_256_CBC_SHA256");
        ciphers.add("TLS_DHE_RSA_WITH_AES_128_CBC_SHA256");
        ciphers.add("TLS_DHE_DSS_WITH_AES_256_CBC_SHA256");
        ciphers.add("TLS_DHE_DSS_WITH_AES_128_CBC_SHA256");

        return ciphers;
    }

    private void writeTlsConfigToApplicationYml() {
        applicationYamlHandler.setAt("server/port", "8443");
        applicationYamlHandler.setAt("server/ssl/enabled", "true");
        applicationYamlHandler.setAt("server/ssl/ciphers", checkboxGroup.getSelectedItems());
        applicationYamlHandler.setAt("server/ssl/protocols", "TLSv1.3");

        if (listBox.getValue().equals(TLS_V1_3_AND_V1_2_PROTOCOL))
            applicationYamlHandler.setAt("server/ssl/protocols", "TLSv1.2");

        applicationYamlHandler.writeYamlToRessources();
        System.out.println("Write tls config to application yml file");
    }
}
