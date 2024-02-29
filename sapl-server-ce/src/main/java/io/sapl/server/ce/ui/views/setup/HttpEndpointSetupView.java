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
@PageTitle("HTTP Endpoint Setup")
@Route(value = HttpEndpointSetupView.ROUTE, layout = SetupLayout.class)
@Conditional(SetupNotFinishedCondition.class)
public class HttpEndpointSetupView extends EndpointSetupView {
    public static final String ROUTE = "/setup/http";

    @Override
    boolean getSaveConfigBtnState() {
        return applicationConfigService.getHttpEndpoint().getEnabled();
    }

    @Override
    void setSaveConfigBtnState(boolean enable) {
        applicationConfigService.getHttpEndpoint().setEnabled(enable);
    }

    @Override
    String getEnabledSslProtocols() {
        return applicationConfigService.getHttpEndpoint().getEnabledSslProtocols();
    }

    @Override
    void setEnabledSslProtocols(String protocols) {
        applicationConfigService.getHttpEndpoint().setEnabledSslProtocols(protocols);
    }

    @Override
    String getKeyStoreType() {
        return applicationConfigService.getHttpEndpoint().getKeyStoreType();
    }

    @Override
    void setKeyStoreType(String keyStoreType) {
        applicationConfigService.getHttpEndpoint().setKeyStoreType(keyStoreType);
    }

    @Override
    String getAdr() {
        return applicationConfigService.getHttpEndpoint().getAdr();
    }

    @Override
    void setAdr(String adr) {
        applicationConfigService.getHttpEndpoint().setAddress(adr);
    }

    @Override
    int getPort() {
        return applicationConfigService.getHttpEndpoint().getPort();
    }

    @Override
    void setPort(int port) {
        applicationConfigService.getHttpEndpoint().setPort(port);
    }

    @Override
    String getKeyStore() {
        return applicationConfigService.getHttpEndpoint().getKeyStore();
    }

    @Override
    void setKeyStore(String keyStore) {
        applicationConfigService.getHttpEndpoint().setKeyStore(keyStore);
    }

    @Override
    String getKeyAlias() {
        return applicationConfigService.getHttpEndpoint().getKeyAlias();
    }

    @Override
    void setKeyAlias(String keyAlias) {
        applicationConfigService.getHttpEndpoint().setKeyAlias(keyAlias);
    }

    @Override
    String getKeyStorePassword() {
        return applicationConfigService.getHttpEndpoint().getKeyStorePassword();
    }

    @Override
    void setKeyStorePassword(String keyStorePassword) {
        applicationConfigService.getHttpEndpoint().setKeyStorePassword(keyStorePassword);
    }

    @Override
    String getKeyPassword() {
        return applicationConfigService.getHttpEndpoint().getKeyPassword();
    }

    @Override
    void setKeyPassword(String keyPassword) {
        applicationConfigService.getHttpEndpoint().setKeyPassword(keyPassword);
    }

    @Override
    Set<String> getSelectedCiphers() {
        return applicationConfigService.getHttpEndpoint().getSelectedCiphers();
    }

    @Override
    void setSelectedCiphers(Set<String> selectedCiphers) {
        applicationConfigService.getHttpEndpoint().setCiphers(selectedCiphers);
    }

    @Override
    void writeConfigToApplicationYml() throws IOException {
        applicationConfigService.persistHttpEndpointConfig();
        applicationConfigService.getHttpEndpoint().setSaved(true);
    }
}
