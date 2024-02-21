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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.data.value.ValueChangeMode;
import io.sapl.server.ce.setup.ApplicationYmlHandler;
import io.sapl.server.ce.ui.utils.ConfirmUtils;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class EndpointSetupView extends VerticalLayout {

    public static final String    ROUTE                      = "/setup/rsocket";
    protected static final String TLS_V1_3_PROTOCOL          = "TLSv1.3";
    private static final String   TLS_V1_3_AND_V1_2_PROTOCOL = "TLSv1.3 + TLSv1.2";
    private static final String   TLS_DISABELD               = "Disable TLS";
    static final String           TLS_AES_128_GCM_SHA256     = "TLS_AES_128_GCM_SHA256";
    protected static final String TLS_AES_256_GCM_SHA384     = "TLS_AES_256_GCM_SHA384";
    protected static final String PKCS12                     = "PKCS12";

    @Autowired
    ApplicationYmlHandler applicationYmlHandler;

    private final TextField                adr                   = new TextField("Address");
    private final IntegerField             port                  = new IntegerField("Port");
    private final TextField                keyStore              = new TextField("Key store path");
    private final TextField                keyAlias              = new TextField("Key alias");
    private final PasswordField            keyStorePassword      = new PasswordField("Key store password");
    private final PasswordField            keyPassword           = new PasswordField("Key password");
    private final RadioButtonGroup<String> enabledSslProtocols   = new RadioButtonGroup<>("Enabled tls protocols");
    private final RadioButtonGroup<String> keyStoreType          = new RadioButtonGroup<>("Key Store Type");
    private final CheckboxGroup<String>    ciphers               = new CheckboxGroup<>("TLS ciphers");
    private final Button                   validatKeyStoreSecret = new Button("Validate keystore settings");
    private final Button                   tlsSaveConfig         = new Button("Save Configuration");
    private final Span                     tlsDisabledWarning    = new Span();

    abstract boolean getSaveConfigBtnState();

    abstract void setSaveConfigBtnState(boolean enable);

    abstract String getPathPrefix();

    abstract String getEnabledSslProtocols();

    abstract void setEnabledSslProtocols(String protocols);

    abstract String getKeyStoreType();

    abstract void setKeyStoreType(String keyStoreType);

    abstract String getAdr();

    abstract void setAdr(String adr);

    abstract int getPort();

    abstract void setPort(int port);

    abstract String getKeyStore();

    abstract void setKeyStore(String keyStore);

    abstract String getKeyAlias();

    abstract void setKeyAlias(String keyAlias);

    abstract String getKeyStorePassword();

    abstract void setKeyStorePassword(String keyStorePassword);

    abstract String getKeyPassword();

    abstract void setKeyPassword(String keyPassword);

    abstract Set<String> getSelectedCiphers();

    abstract void setSelectedCiphers(Set<String> keyPassword);

    @PostConstruct
    private void init() {
        add(getLayout());
    }

    public Component getLayout() {
        enabledSslProtocols.setItems(TLS_V1_3_PROTOCOL, TLS_V1_3_AND_V1_2_PROTOCOL, TLS_DISABELD);
        enabledSslProtocols.setValue(getEnabledSslProtocols());
        enabledSslProtocols.addValueChangeListener(e -> {
            setTlsFieldsVisible(!e.getValue().equals(TLS_DISABELD));
            setEnabledSslProtocols(e.getValue());
            setEnableTlsConfigBtn();
        });

        setEnableTlsConfigBtn();
        tlsSaveConfig.addClickListener(e -> writeTlsConfigToApplicationYml());
        validatKeyStoreSecret.addClickListener(e -> openKeyStore());

        adr.setPlaceholder("localhost");
        adr.setRequiredIndicatorVisible(true);
        adr.setValueChangeMode(ValueChangeMode.EAGER);
        adr.setValue(getAdr());
        adr.addValueChangeListener(e -> {
            setAdr(e.getValue());
            setEnableTlsConfigBtn();
        });

        port.setPlaceholder("7000");
        port.setRequiredIndicatorVisible(true);
        port.setValueChangeMode(ValueChangeMode.EAGER);
        port.setMin(0);
        port.setMax(65535);
        port.setHelperText("Rang from 0 to 65535");
        port.setValue(getPort());
        port.addValueChangeListener(e -> {
            if (e.getValue() != null)
                setPort(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStore.setPlaceholder("file:config/keystore.p12");
        keyStore.setRequiredIndicatorVisible(true);
        keyStore.setValueChangeMode(ValueChangeMode.EAGER);
        keyStore.setValue(getKeyStore());
        keyStore.addValueChangeListener(e -> {
            setSaveConfigBtnState(false);
            setKeyStore(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyAlias.setPlaceholder("netty");
        keyAlias.setValueChangeMode(ValueChangeMode.EAGER);
        keyAlias.setValue(getKeyAlias());
        keyAlias.addValueChangeListener(e -> {
            setKeyAlias(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStoreType.setItems(getKeyStoreTypes());
        keyStoreType.setRequiredIndicatorVisible(true);
        keyStoreType.setValue(getKeyStoreType());
        keyStoreType.addValueChangeListener(e -> {
            setSaveConfigBtnState(false);
            setKeyStoreType(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyStorePassword.setValueChangeMode(ValueChangeMode.EAGER);
        keyStorePassword.setRequiredIndicatorVisible(true);
        keyStorePassword.setValue(getKeyStorePassword());
        keyStorePassword.addValueChangeListener(e -> {
            setSaveConfigBtnState(false);
            setKeyStorePassword(e.getValue());
            setEnableTlsConfigBtn();
        });

        keyPassword.setValueChangeMode(ValueChangeMode.EAGER);
        keyPassword.setRequiredIndicatorVisible(true);
        keyPassword.setValue(getKeyPassword());
        keyPassword.addValueChangeListener(e -> {
            setSaveConfigBtnState(false);
            setKeyPassword(e.getValue());
            setEnableTlsConfigBtn();
        });

        ciphers.setItems(getCiphers());
        ciphers.addSelectionListener(e -> {
            checkIfAtLeastOneCipherOptionSelected();
            setSelectedCiphers(e.getValue());
        });
        ciphers.select(getSelectedCiphers());
        ciphers.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        add(ciphers);

        VerticalLayout keyLayout = new VerticalLayout();
        keyLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        keyLayout.setPadding(false);
        keyLayout.add(keyStore);
        keyLayout.add(keyStorePassword);
        keyLayout.add(keyPassword);
        keyLayout.add(keyAlias);
        keyLayout.add(validatKeyStoreSecret);

        tlsDisabledWarning.setText("Note: Do not use the option \"Disable TLS\" in production.\n"
                + "This option may open the server to malicious probing and exfiltration attempts through"
                + "the authorization endpoints, potentially resulting in unauthorized access to your"
                + "organization's data, depending on your policies.");
        tlsDisabledWarning.getStyle().set("color", "var(--lumo-error-text-color)");
        tlsDisabledWarning.setVisible(enabledSslProtocols.getValue().equals(TLS_DISABELD));

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
        keyTypes.add("PKCS12");
        keyTypes.add("JCEKS");
        keyTypes.add("JKS");

        return keyTypes;
    }

    private void checkIfAtLeastOneCipherOptionSelected() {
        setEnableTlsConfigBtn();

        if (ciphers.getSelectedItems().isEmpty()) {
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

    private void setEnableTlsConfigBtn() {
        int portNumber = port.getValue() != null ? port.getValue() : -1;

        boolean tls_enabled           = !enabledSslProtocols.getValue().equals(TLS_DISABELD);
        boolean adrAndPortInputExists = !adr.getValue().isEmpty() && portNumber > 0;
        boolean btnEnabled            = tls_enabled
                ? adrAndPortInputExists && !ciphers.getSelectedItems().isEmpty() && !keyStore.getValue().isEmpty()
                        && !keyStorePassword.getValue().isEmpty() && !keyPassword.getValue().isEmpty()
                        && getSaveConfigBtnState()
                : adrAndPortInputExists;

        tlsSaveConfig.setEnabled(btnEnabled);
    }

    void writeTlsConfigToApplicationYml() {
        applicationYmlHandler.setAt(getPathPrefix() + "port", port.getValue());
        applicationYmlHandler.setAt(getPathPrefix() + "address", adr.getValue());

        boolean tls_enabled = !enabledSslProtocols.getValue().equals(TLS_DISABELD);
        applicationYmlHandler.setAt(getPathPrefix() + "ssl/enabled", tls_enabled);

        if (tls_enabled) {
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/key-store-type", keyStoreType.getValue());
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/key-store", keyStore.getValue());
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/key-store-password", keyStorePassword.getValue());
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/key-password", keyPassword.getValue());
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/key-alias", keyAlias.getValue());

            applicationYmlHandler.setAt(getPathPrefix() + "ssl/ciphers", ciphers.getSelectedItems());
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/enabled-protocols",
                    enabledSslProtocols.getValue().split(" \\+ "));
            applicationYmlHandler.setAt(getPathPrefix() + "ssl/protocols", TLS_V1_3_PROTOCOL);
        }

        try {
            applicationYmlHandler.saveYmlFiles();
            ConfirmUtils.inform("saved", "Endpoint setup successfully saved");
        } catch (IOException ioe) {
            ConfirmUtils.inform("IO-Error",
                    "Error while writing application.yml-File. Please make sure that the file is not in use and can be written. Otherwise configure the application.yml-file manually. Error: "
                            + ioe.getMessage());
        }
    }

    private void openKeyStore() {
        if (keyStorePathInvalid()) {
            ConfirmUtils.inform("Key store path invalid", "Key store path must begin with \"file:\"");
            return;
        }

        char[] pwdArray = keyStorePassword.getValue().toCharArray();

        try {
            KeyStore ks = KeyStore.getInstance(keyStoreType.getValue());
            ks.load(new FileInputStream(keyStore.getValue().substring(5)), pwdArray);
            setSaveConfigBtnState(true);
            setEnableTlsConfigBtn();
            ConfirmUtils.inform("success", "Keystore password valid");
        } catch (CertificateException e) {
            ConfirmUtils.inform("Certificate fault", e.getMessage());
        } catch (KeyStoreException e) {
            ConfirmUtils.inform("Key store fault", e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            ConfirmUtils.inform("No such algorithm exception", e.getMessage());
        } catch (FileNotFoundException e) {
            ConfirmUtils.inform("File not found", e.getMessage());
        } catch (IOException e) {
            ConfirmUtils.inform("Error", e.getMessage());
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
        validatKeyStoreSecret.setVisible(visible);
        tlsDisabledWarning.setVisible(!visible);
    }
}
