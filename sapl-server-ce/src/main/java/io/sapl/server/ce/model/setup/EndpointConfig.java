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

package io.sapl.server.ce.model.setup;

import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EndpointConfig {
    public static final String TLS_V1_3_PROTOCOL          = "TLSv1.3";
    public static final String TLS_V1_2_PROTOCOL          = "TLSv1.2";
    public static final String TLS_V1_3_AND_V1_2_PROTOCOL = "TLSv1.3 + TLSv1.2";
    public static final String TLS_DISABELD               = "Disable TLS";
    public static final String KEY_STORE_TYPE_PKCS12      = "PKCS12";
    public static final String KEY_STORE_TYPE_JCEKS       = "JCEKS";
    public static final String KEY_STORE_TYPE_JKS         = "JKS";
    public static final String TLS_AES_128_GCM_SHA256     = "TLS_AES_128_GCM_SHA256";
    public static final String TLS_AES_256_GCM_SHA384     = "TLS_AES_256_GCM_SHA384";

    final String portPath;
    final String addressPath;
    final String transportPath;
    final String sslEnabledPath;
    final String sslKeyStoreTypePath;
    final String sslKeyStorePath;
    final String sslKeyStorePasswordPath;
    final String sslKeyPasswordPath;
    final String sslKeyAliasPath;
    final String sslCiphersPath;
    final String sslEnabledProtocolsPath;
    final String sslProtocolsPath;

    @Getter
    @Setter
    private boolean     saved               = false;
    @Setter
    private boolean     enabled             = false;
    @Setter
    @Getter
    private String      address             = "";
    @Setter
    @Getter
    private int         port                = 1;
    @Getter
    private Boolean     sslEnabled          = false;
    @Getter
    private String      enabledSslProtocols = "";
    @Getter
    private String      keyStoreType        = "";
    @Getter
    private String      keyStore            = "";
    @Getter
    private String      keyPassword         = "";
    @Getter
    private String      keyStorePassword    = "";
    @Getter
    private String      keyAlias            = "";
    @Getter
    private Set<String> selectedCiphers     = new HashSet<>(Set.of(TLS_AES_128_GCM_SHA256, TLS_AES_256_GCM_SHA384));

    public EndpointConfig(String prefix, int port) {
        addressPath = prefix + "address";
        portPath = prefix + "port";
        transportPath = prefix + "transport";

        sslEnabledPath = prefix + "ssl.enabled";
        sslKeyStoreTypePath = prefix + "ssl.key-store-type";
        sslKeyStorePath = prefix + "ssl.key-store";
        sslKeyStorePasswordPath = prefix + "ssl.key-store-password";
        sslKeyPasswordPath = prefix + "ssl.key-password";
        sslKeyAliasPath = prefix + "ssl.key-alias";
        sslCiphersPath = prefix + "ssl.ciphers";
        sslEnabledProtocolsPath = prefix + "ssl.enabled-protocols";
        sslProtocolsPath = prefix + "ssl.protocols";

        this.port = port;
    }

    public void setSslEnabled(boolean sslEnabled) {
        this.sslEnabled = sslEnabled;
        if (!sslEnabled)
            enabledSslProtocols = EndpointConfig.TLS_DISABELD;
    }

    public void setEnabledSslProtocols(String s) {
        enabledSslProtocols = EndpointConfig.TLS_DISABELD;

        if (s.contains(TLS_V1_3_PROTOCOL))
            enabledSslProtocols = EndpointConfig.TLS_V1_3_PROTOCOL;
        if (s.contains(TLS_V1_3_PROTOCOL) && s.contains(TLS_V1_2_PROTOCOL))
            enabledSslProtocols = EndpointConfig.TLS_V1_3_AND_V1_2_PROTOCOL;

        this.sslEnabled = !enabledSslProtocols.equals(EndpointConfig.TLS_DISABELD);
    }

    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
        if (this.keyStoreType.isEmpty())
            this.keyStoreType = KEY_STORE_TYPE_PKCS12;
    }

    public void setKeyStore(String keyStore) {
        this.keyStore = keyStore;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
    }

    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public void setCiphers(Object obj) {
        if (obj instanceof String) {
            String ciphers = obj.toString();

            if (ciphers.isEmpty())
                return;

            ciphers = ciphers.replace("[", "").replace("]", "");
            selectedCiphers.clear();
            selectedCiphers = Stream.of(ciphers.trim().split("\\s*,\\s*")).collect(Collectors.toSet());
        }
    }

    public void setCiphers(Set<String> selectedCiphers) {
        this.selectedCiphers = selectedCiphers;
    }

    public boolean getEnabled() {
        return enabled;
    }

}
