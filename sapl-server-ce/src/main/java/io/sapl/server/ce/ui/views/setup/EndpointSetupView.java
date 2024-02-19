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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import io.sapl.server.ce.setup.ApplicationYamlHandler;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class EndpointSetupView extends VerticalLayout {

    public static final String ROUTE = "/setup/rsocket";

    @Autowired
    ApplicationYamlHandler                 applicationYamlHandler;
    private static final String            TLS_V1_3_PROTOCOL          = "TLSv1.3";
    private static final String            TLS_V1_3_AND_V1_2_PROTOCOL = "TLSv1.3 + TLSv1.2";
    private static final String            TLS_AES_128_GCM_SHA256     = "TLS_AES_128_GCM_SHA256";
    private static final String            TLS_AES_256_GCM_SHA384     = "TLS_AES_256_GCM_SHA384";
    private static final String            PKCS12                     = "PKCS12";
    private final TextField                adr                        = new TextField("Address");
    private final TextField                port                       = new TextField("Port");
    private final TextField                keyStore                   = new TextField("Key store");
    private final TextField                keyAlias                   = new TextField("Key alias");
    private final PasswordField            keyStorePassword           = new PasswordField("Key store password");
    private final PasswordField            keyPassword                = new PasswordField("Key password");
    private final RadioButtonGroup<String> enabledSslProtocols        = new RadioButtonGroup<>("Enabled ssl protocols");
    private final RadioButtonGroup<String> keyStoreType               = new RadioButtonGroup<>("Key Store Type");
    private final CheckboxGroup<String>    checkboxGroup              = new CheckboxGroup<>("TLS ciphers");
    private final Button                   tlsSaveConfig              = new Button("Save Configuration");

    abstract String getPathPrefix();

    @PostConstruct
    private void init() {
        add(getLayout());

    }

    public Component getLayout() {
        enabledSslProtocols.setItems(TLS_V1_3_PROTOCOL, TLS_V1_3_AND_V1_2_PROTOCOL);
        enabledSslProtocols.setValue(TLS_V1_3_PROTOCOL);

        tlsSaveConfig.addClickListener(e -> writeTlsConfigToApplicationYml());

        adr.setPlaceholder("localhost");
        port.setPlaceholder("7000");
        keyStore.setPlaceholder("file:config/keystore.p12");
        keyAlias.setPlaceholder("netty");

        keyStoreType.setItems(getKeyStoreTypes());
        keyStoreType.setValue(PKCS12);

        checkboxGroup.setItems(getCiphers());
        checkboxGroup.addSelectionListener(e -> checkIfAtLeastOneCipherOptionSelected());

        checkboxGroup.select(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384);
        checkboxGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        add(checkboxGroup);

        FormLayout tlsLayout = new FormLayout(adr, port, keyStoreType, keyStore, keyStorePassword, keyPassword,
                keyAlias, enabledSslProtocols, checkboxGroup, tlsSaveConfig);
        tlsLayout.setColspan(enabledSslProtocols, 2);
        tlsLayout.setColspan(checkboxGroup, 2);
        tlsLayout.setColspan(tlsSaveConfig, 2);

        return tlsLayout;
    }

    private static List<String> getCiphers() {
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

    private static List<String> getKeyStoreTypes() {
        List<String> keyTypes = new ArrayList<>();
        keyTypes.add("PKCS12");
        keyTypes.add("JCEKS");
        keyTypes.add("JKS");

        return keyTypes;
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

    void writeTlsConfigToApplicationYml() {
        applicationYamlHandler.setAt(getPathPrefix() + "port", port.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "address", adr.getValue());

        applicationYamlHandler.setAt(getPathPrefix() + "ssl/enabled", "true");

        applicationYamlHandler.setAt(getPathPrefix() + "ssl/key-store-type", keyStoreType.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/key-store", keyStore.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/key-store-password", keyStorePassword.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/key-password", keyPassword.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/key-alias", keyAlias.getValue());

        applicationYamlHandler.setAt(getPathPrefix() + "ssl/ciphers", checkboxGroup.getSelectedItems());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/enabled-protocols", enabledSslProtocols.getValue());
        applicationYamlHandler.setAt(getPathPrefix() + "ssl/protocols", TLS_V1_3_PROTOCOL);

        try {
            applicationYamlHandler.saveYamlFiles();
            ConfirmUtils.inform("saved", "Endpoint setup successfully saved");
        } catch (IOException ioe) {
            ConfirmUtils.inform("IO-Error",
                    "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                            + ioe.getMessage());
        }
    }
}
