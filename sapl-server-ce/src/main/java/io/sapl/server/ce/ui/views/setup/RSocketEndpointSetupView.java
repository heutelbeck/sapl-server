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

import java.io.IOException;
import java.util.Set;

@AnonymousAllowed
@PageTitle("RSocket Endpoint Setup")
@Route(value = RSocketEndpointSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class RSocketEndpointSetupView extends EndpointSetupView {
    public static final String ROUTE = "/setup/rsocket";

    @Override
    boolean getSaveConfigBtnState() {
        return applicationConfigService.getRsocketEndpoint().getEnabled();
    }

    @Override
    void setSaveConfigBtnState(boolean enabled) {
        applicationConfigService.getRsocketEndpoint().setEnabled(enabled);
    }

    @Override
    String getEnabledSslProtocols() {
        return applicationConfigService.getRsocketEndpoint().getEnabledSslProtocols();
    }

    @Override
    void setEnabledSslProtocols(String protocols) {
        applicationConfigService.getRsocketEndpoint().setEnabledSslProtocols(protocols);
    }

    @Override
    String getKeyStoreType() {
        return applicationConfigService.getRsocketEndpoint().getKeyStoreType();
    }

    @Override
    void setKeyStoreType(String keyStoreType) {
        applicationConfigService.getRsocketEndpoint().setKeyStoreType(keyStoreType);
    }

    @Override
    String getAdr() {
        return applicationConfigService.getRsocketEndpoint().getAdr();
    }

    @Override
    void setAdr(String adr) {
        applicationConfigService.getRsocketEndpoint().setAddress(adr);
    }

    @Override
    int getPort() {
        return applicationConfigService.getRsocketEndpoint().getPort();
    }

    @Override
    void setPort(int port) {
        applicationConfigService.getRsocketEndpoint().setPort(port);
    }

    @Override
    String getKeyStore() {
        return applicationConfigService.getRsocketEndpoint().getKeyStore();
    }

    @Override
    void setKeyStore(String keyStore) {
        applicationConfigService.getRsocketEndpoint().setKeyStore(keyStore);
    }

    @Override
    String getKeyAlias() {
        return applicationConfigService.getRsocketEndpoint().getKeyAlias();
    }

    @Override
    void setKeyAlias(String keyAlias) {
        applicationConfigService.getRsocketEndpoint().setKeyAlias(keyAlias);
    }

    @Override
    String getKeyStorePassword() {
        return applicationConfigService.getRsocketEndpoint().getKeyStorePassword();
    }

    @Override
    void setKeyStorePassword(String keyStorePassword) {
        applicationConfigService.getRsocketEndpoint().setKeyStorePassword(keyStorePassword);
    }

    @Override
    String getKeyPassword() {
        return applicationConfigService.getRsocketEndpoint().getKeyPassword();
    }

    @Override
    void setKeyPassword(String keyPassword) {
        applicationConfigService.getRsocketEndpoint().setKeyPassword(keyPassword);
    }

    @Override
    Set<String> getSelectedCiphers() { return applicationConfigService.getRsocketEndpoint().getSelectedCiphers(); }

    @Override
    void setSelectedCiphers(Set<String> selectedCiphers) {
        applicationConfigService.getRsocketEndpoint().setCiphers(selectedCiphers);
    }

    @Override
    void writeConfigToApplicationYml() throws IOException {
        applicationConfigService.persistRsocketEndpointConfig();
        applicationConfigService.getRsocketEndpoint().setSaved(true);
    }
}
