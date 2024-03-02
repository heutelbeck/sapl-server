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

import com.google.common.net.InetAddresses;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.sapl.server.ce.model.setup.ApplicationConfigService;
import io.sapl.server.ce.model.setup.EndpointConfig;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import io.sapl.server.ce.ui.utils.ErrorNotificationUtils;
import jakarta.annotation.PostConstruct;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

public abstract class EndpointSetupView extends VerticalLayout {

    public static final String  ROUTE         = "/setup/rsocket";
    private static final String SUCCESS_COLOR = "var(--lumo-success-color)";
    private static final String ERROR_COLOR   = "var(--lumo-error-color)";

    transient ApplicationConfigService applicationConfigService;
    transient EndpointConfig           endpointConfig;

    private final TextField                adr                    = new TextField("Address");
    private final IntegerField             port                   = new IntegerField("Port");
    private final TextField                keyStore               = new TextField("Key store path");
    private final TextField                keyAlias               = new TextField("Key alias");
    private final PasswordField            keyStorePassword       = new PasswordField("Key store password");
    private final PasswordField            keyPassword            = new PasswordField("Key password");
    private final RadioButtonGroup<String> enabledSslProtocols    = new RadioButtonGroup<>("Enabled tls protocols");
    private final RadioButtonGroup<String> keyStoreType           = new RadioButtonGroup<>("Key Store Type");
    private final CheckboxGroup<String>    ciphers                = new CheckboxGroup<>("TLS ciphers");
    private final Button                   validateKeyStoreSecret = new Button("Validate keystore settings");
    private final Button                   tlsSaveConfig          = new Button("Save Configuration");
    private final Span                     tlsDisabledWarning     = new Span();
    private Span                           inputValidText;

    abstract void persistConfig() throws IOException;

    @PostConstruct
    private void init() {
        add(getLayout());
    }

    public Component getLayout() {
        enabledSslProtocols.setItems(EndpointConfig.TLS_V1_3_PROTOCOL, EndpointConfig.TLS_V1_3_AND_V1_2_PROTOCOL,
                EndpointConfig.TLS_DISABELD);
        enabledSslProtocols.setValue(endpointConfig.getEnabledSslProtocols());
        enabledSslProtocols.addValueChangeListener(e -> {
            setTlsFieldsVisible(!e.getValue().equals(EndpointConfig.TLS_DISABELD));
            endpointConfig.setEnabledSslProtocols(e.getValue());
            setEnableTlsConfigBtn();
        });

        setEnableTlsConfigBtn();
        setTlsFieldsVisible(!enabledSslProtocols.getValue().equals(EndpointConfig.TLS_DISABELD));
        tlsSaveConfig.addClickListener(e -> {
            try {
                persistConfig();
                ConfirmUtils.inform("saved", "Endpoint setup successfully saved");
            } catch (IOException ioe) {
                ErrorNotificationUtils.show(
                        "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                                + ioe.getMessage());
            }
        });
        validateKeyStoreSecret.addClickListener(e -> openKeyStore());

        adr.setRequiredIndicatorVisible(true);
        adr.setValueChangeMode(ValueChangeMode.EAGER);
        adr.setValue(endpointConfig.getAddress());
        Div inputValid = new Div();
        inputValidText = new Span();
        inputValid.add(new Text("Input is "), inputValidText);
        adr.setHelperComponent(inputValid);
        adr.addValueChangeListener(e -> adrInputValidationInfo(e.getValue()));
        adr.addBlurListener(event -> {
            if (isValidURI(event.getSource().getValue())) {
                endpointConfig.setAddress(event.getSource().getValue());
                setEnableTlsConfigBtn();
            } else {
                ConfirmUtils.inform("Address Input-Error", "The entry in the address field is no valid ip address!");
            }
        });
        adrInputValidationInfo(adr.getValue());

        port.setRequiredIndicatorVisible(true);
        port.setValueChangeMode(ValueChangeMode.EAGER);
        port.setMin(1);
        port.setMax(65535);
        port.setValue(endpointConfig.getPort());
        port.setHelperText("Rang from 1 to 65535");
        port.addValueChangeListener(e -> {
            if (e.getValue() != null)
                endpointConfig.setPort(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStore.setPlaceholder("file:config/keystore.p12");
        keyStore.setRequiredIndicatorVisible(true);
        keyStore.setValueChangeMode(ValueChangeMode.EAGER);
        keyStore.setValue(endpointConfig.getKeyStore());
        keyStore.addValueChangeListener(e -> {
            endpointConfig.setEnabled(false);
            endpointConfig.setKeyStore(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyAlias.setPlaceholder("netty");
        keyAlias.setRequiredIndicatorVisible(true);
        keyAlias.setValueChangeMode(ValueChangeMode.EAGER);
        keyAlias.setValue(endpointConfig.getKeyAlias());
        keyAlias.addValueChangeListener(e -> {
            endpointConfig.setKeyAlias(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStoreType.setItems(getKeyStoreTypes());
        keyStoreType.setRequiredIndicatorVisible(true);
        keyStoreType.setValue(endpointConfig.getKeyStoreType());
        keyStoreType.addValueChangeListener(e -> {
            endpointConfig.setEnabled(false);
            endpointConfig.setKeyStoreType(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStorePassword.setValueChangeMode(ValueChangeMode.EAGER);
        keyStorePassword.setRequiredIndicatorVisible(true);
        keyStorePassword.setValue(endpointConfig.getKeyStorePassword());
        keyStorePassword.addValueChangeListener(e -> {
            endpointConfig.setEnabled(false);
            endpointConfig.setKeyStorePassword(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyPassword.setValueChangeMode(ValueChangeMode.EAGER);
        keyPassword.setRequiredIndicatorVisible(true);
        keyPassword.setValue(endpointConfig.getKeyPassword());
        keyPassword.addValueChangeListener(e -> {
            endpointConfig.setEnabled(false);
            endpointConfig.setKeyPassword(e.getValue());
            setEnableTlsConfigBtn();
        });

        ciphers.setItems(getCiphers());
        ciphers.addSelectionListener(e -> {
            checkIfAtLeastOneCipherOptionSelected();
            endpointConfig.setCiphers(e.getValue());
        });
        ciphers.select(endpointConfig.getSelectedCiphers());
        ciphers.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        add(ciphers);

        VerticalLayout keyLayout = new VerticalLayout();
        keyLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        keyLayout.setPadding(false);
        keyLayout.add(keyStore);
        keyLayout.add(keyStorePassword);
        keyLayout.add(keyPassword);
        keyLayout.add(keyAlias);
        keyLayout.add(validateKeyStoreSecret);

        tlsDisabledWarning.setText("Note: Do not use the option \"Disable TLS\" in production.\n"
                + "This option may open the server to malicious probing and exfiltration attempts through "
                + "the authorization endpoints, potentially resulting in unauthorized access to your "
                + "organization's data, depending on your policies.");
        tlsDisabledWarning.getStyle().set("color", "var(--lumo-error-text-color)");
        tlsDisabledWarning.setVisible(enabledSslProtocols.getValue().equals(EndpointConfig.TLS_DISABELD));

        FormLayout tlsLayout = new FormLayout(adr, port, enabledSslProtocols, keyStoreType, ciphers, keyLayout,
                tlsDisabledWarning, tlsSaveConfig);
        tlsLayout.setColspan(tlsDisabledWarning, 2);
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
        keyTypes.add(EndpointConfig.KEY_STORE_TYPE_PKCS12);
        keyTypes.add(EndpointConfig.KEY_STORE_TYPE_JCEKS);
        keyTypes.add(EndpointConfig.KEY_STORE_TYPE_JKS);

        return keyTypes;
    }

    private void adrInputValidationInfo(String adr) {
        if (isValidURI(adr)) {
            inputValidText.setText("valid");
            inputValidText.getStyle().setColor(SUCCESS_COLOR);
        } else {
            inputValidText.setText("invalid");
            inputValidText.getStyle().setColor(ERROR_COLOR);
        }
    }

    private boolean isValidURI(String adr) {
        return InetAddresses.isUriInetAddress(adr) || adr.equals("localhost");
    }

    private void checkIfAtLeastOneCipherOptionSelected() {
        setEnableTlsConfigBtn();

        if (ciphers.getSelectedItems().isEmpty())
            ErrorNotificationUtils.show("At least one cipher option must be selected");
    }

    private void setEnableTlsConfigBtn() {
        int portNumber = port.getValue() != null ? port.getValue() : -1;

        boolean tlsEnabled                = !enabledSslProtocols.getValue().equals(EndpointConfig.TLS_DISABELD);
        boolean adrValidAndPortInputExists = isValidURI(adr.getValue()) && portNumber > 0;
        boolean btnEnabled                 = tlsEnabled
                ? adrValidAndPortInputExists && !ciphers.getSelectedItems().isEmpty() && !keyStore.getValue().isEmpty()
                        && !keyStorePassword.getValue().isEmpty() && !keyPassword.getValue().isEmpty()
                        && endpointConfig.getEnabled()
                : adrValidAndPortInputExists;

        tlsSaveConfig.setEnabled(btnEnabled);
    }

    private void openKeyStore() {
        if (keyStorePathInvalid()) {
            ErrorNotificationUtils.show("Key store path invalid: Key store path must begin with \"file:\"");
            return;
        }

        char[] pwdArray = keyStorePassword.getValue().toCharArray();

        try {
            KeyStore ks = KeyStore.getInstance(keyStoreType.getValue());
            ks.load(new FileInputStream(keyStore.getValue().substring(5)), pwdArray);

            if (!ks.containsAlias(keyAlias.getValue())) {
                ErrorNotificationUtils.show("Key alias fault: The given key alias does not exists in this keystore");
                return;
            }

            endpointConfig.setEnabled(true);
            setEnableTlsConfigBtn();
            ConfirmUtils.inform("success", "Keystore settings valid");
        } catch (CertificateException e) {
            ErrorNotificationUtils.show("Certificate fault: " + e.getMessage());
        } catch (KeyStoreException e) {
            ErrorNotificationUtils.show("Key store fault: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            ErrorNotificationUtils.show("No such algorithm exception: " + e.getMessage());
        } catch (FileNotFoundException e) {
            ErrorNotificationUtils.show("File not found: " + e.getMessage());
        } catch (IOException e) {
            ErrorNotificationUtils.show("Error: " + e.getMessage());
        }
    }

    private boolean keyStorePathInvalid() {
        return keyStore.getValue() == null || !keyStore.getValue().startsWith("file:");
    }

    private void setTlsFieldsVisible(boolean visible) {
        keyStore.setVisible(visible);
        keyAlias.setVisible(visible);
        keyStoreType.setVisible(visible);
        keyStorePassword.setVisible(visible);
        keyPassword.setVisible(visible);
        ciphers.setVisible(visible);
        validateKeyStoreSecret.setVisible(visible);
        tlsDisabledWarning.setVisible(!visible);
    }
}
