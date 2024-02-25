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

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import io.sapl.server.ce.model.setup.condition.SetupNotFinishedCondition;
import io.sapl.server.ce.ui.views.SetupLayout;
import org.springframework.context.annotation.Conditional;

import java.util.HashSet;
import java.util.Set;

@AnonymousAllowed
@PageTitle("RSocket Endpoint Setup")
@Route(value = RSocketEndpointSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class RSocketEndpointSetupView extends EndpointSetupView {
    public static final String  ROUTE       = "/setup/rsocket";
    private static final String PATH_PREFIX = "spring.rsocket.server/";

    private static boolean     enableSaveConfigBtn;
    private static String      enabledSslProtocols = TLS_V1_3_PROTOCOL;
    private static String      keyStoreType        = PKCS12;
    private static String      adr                 = "";
    private static int         port                = 7000;
    private static String      keyStore            = "";
    private static String      keyAlias            = "";
    private static String      keyStorePassword    = "";
    private static String      keyPassword         = "";
    private static Set<String> selectedCiphers     = new HashSet<String>(
            Set.of(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384));

    @Override
    boolean getSaveConfigBtnState() {
        return enableSaveConfigBtn;
    }

    @Override
    void setSaveConfigBtnState(boolean enable) {
        enableSaveConfigBtn = enable;
    }

    @Override
    String getPathPrefix() {
        return PATH_PREFIX;
    }

    @Override
    void writeTlsConfigToApplicationYml() {
        super.writeTlsConfigToApplicationYml();
        applicationConfigService.setAt(getPathPrefix() + "transport", "tcp");
    }

    @Override
    String getEnabledSslProtocols() {
        return enabledSslProtocols;
    }

    @Override
    void setEnabledSslProtocols(String protocols) {
        enabledSslProtocols = protocols;
    }

    @Override
    String getKeyStoreType() {
        return keyStoreType;
    }

    @Override
    void setKeyStoreType(String keyStoreType) {
        RSocketEndpointSetupView.keyStoreType = keyStoreType;
    }

    @Override
    String getAdr() {
        return adr;
    }

    @Override
    void setAdr(String adr) {
        RSocketEndpointSetupView.adr = adr;
    }

    @Override
    int getPort() {
        return port;
    }

    @Override
    void setPort(int port) {
        RSocketEndpointSetupView.port = port;
    }

    @Override
    String getKeyStore() {
        return keyStore;
    }

    @Override
    void setKeyStore(String keyStore) {
        RSocketEndpointSetupView.keyStore = keyStore;
    }

    @Override
    String getKeyAlias() {
        return keyAlias;
    }

    @Override
    void setKeyAlias(String keyAlias) {
        RSocketEndpointSetupView.keyAlias = keyAlias;
    }

    @Override
    String getKeyStorePassword() {
        return keyStorePassword;
    }

    @Override
    void setKeyStorePassword(String keyStorePassword) {
        RSocketEndpointSetupView.keyStorePassword = keyStorePassword;
    }

    @Override
    String getKeyPassword() {
        return keyPassword;
    }

    @Override
    void setKeyPassword(String keyPassword) {
        RSocketEndpointSetupView.keyPassword = keyPassword;
    }

    @Override
    Set<String> getSelectedCiphers() {
        return selectedCiphers;
    }

    @Override
    void setSelectedCiphers(Set<String> selectedCiphers) {
        RSocketEndpointSetupView.selectedCiphers = selectedCiphers;
    }
}
