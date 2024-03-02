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

import com.google.common.net.InetAddresses;
import lombok.Getter;
import lombok.Setter;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.stream.Collectors;

public class EndpointConfig {
    public static final String TLS_V1_3_PROTOCOL          = "TLSv1.3";
    public static final String TLS_V1_2_PROTOCOL          = "TLSv1.2";
    public static final String TLS_V1_3_AND_V1_2_PROTOCOL = "TLSv1.3 + TLSv1.2";
    public static final String TLS_DISABELD               = "Disable TLS";
    public static final String KEY_STORE_TYPE_PKCS12      = "PKCS12";
    public static final String KEY_STORE_TYPE_JCEKS       = "JCEKS";
    public static final String KEY_STORE_TYPE_JKS         = "JKS";

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
    private boolean               saved               = false;
    @Setter
    @Getter
    private String                address             = "";
    @Setter
    @Getter
    private int                   port;
    @Getter
    private Boolean               sslEnabled          = false;
    @Getter
    private String                enabledSslProtocols = "";
    @Getter
    private String                keyStoreType        = "";
    @Getter
    private String                keyStore            = "";
    @Getter
    private String                keyPassword         = "";
    @Getter
    private String                keyStorePassword    = "";
    @Getter
    private String                keyAlias            = "";
    @Getter
    private Set<SupportedCiphers> ciphers             = new HashSet<>(
            Set.of(SupportedCiphers.TLS_AES_128_GCM_SHA256, SupportedCiphers.TLS_AES_256_GCM_SHA384));
    private boolean               validKeystoreConfig = false;

    public EndpointConfig(String prefix, int port) {

        addressPath   = prefix + "address";
        portPath      = prefix + "port";
        transportPath = prefix + "transport";

        sslEnabledPath          = prefix + "ssl.enabled";
        sslKeyStoreTypePath     = prefix + "ssl.key-store-type";
        sslKeyStorePath         = prefix + "ssl.key-store";
        sslKeyStorePasswordPath = prefix + "ssl.key-store-password";
        sslKeyPasswordPath      = prefix + "ssl.key-password";
        sslKeyAliasPath         = prefix + "ssl.key-alias";
        sslCiphersPath          = prefix + "ssl.ciphers";
        sslEnabledProtocolsPath = prefix + "ssl.enabled-protocols";
        sslProtocolsPath        = prefix + "ssl.protocols";

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
        if (!keyStoreType.equals(this.keyStoreType)) {
            this.validKeystoreConfig = false;
            this.keyStoreType        = keyStoreType;
        }
        if (this.keyStoreType.isEmpty())
            this.keyStoreType = KEY_STORE_TYPE_PKCS12;
    }

    public void setKeyStore(String keyStore) {
        if (!keyStore.equals(this.keyStore)) {
            this.validKeystoreConfig = false;
            this.keyStore            = keyStore;
        }
    }

    public void setKeyStorePassword(String keyStorePassword) {
        if (!keyStorePassword.equals(this.keyStorePassword)) {
            this.validKeystoreConfig = false;
            this.keyStorePassword    = keyStorePassword;
        }
    }

    public void setKeyPassword(String keyPassword) {
        if (!keyPassword.equals(this.keyPassword)) {
            this.validKeystoreConfig = false;
            this.keyPassword         = keyPassword;
        }
    }

    public void setKeyAlias(String keyAlias) {
        if (!keyAlias.equals(this.keyAlias)) {
            this.validKeystoreConfig = false;
            this.keyAlias            = keyAlias;
        }
    }

    public void setCiphers(Set<SupportedCiphers> ciphers) {
        this.ciphers = ciphers;
    }

    public void setCiphers(List<String> ciphers) {
        this.ciphers = ciphers.stream().map(SupportedCiphers::valueOf).collect(Collectors.toSet());
    }

    public void setCiphers(String ciphers) {
        this.ciphers = Arrays.stream(ciphers.split(",")).map(SupportedCiphers::valueOf).collect(Collectors.toSet());
    }

    public boolean isValidConfig() {
        return isValidURI() && isValidPort() && isValidProtocolConfig();
    }

    public boolean isValidURI() {
        return InetAddresses.isUriInetAddress(this.address) || this.address.equals("localhost");
    }

    public boolean isValidPort() {
        return this.port > 0 && this.port < 65535;
    }

    public boolean isValidProtocolConfig() {
        if (Boolean.TRUE.equals(sslEnabled)) {
            return this.validKeystoreConfig && !this.ciphers.isEmpty();
        }
        return true;
    }

    public boolean portAndProtocolsMatch() {
        return (sslEnabled && this.port != 80 && this.port != 8080)
                || (!sslEnabled && this.port != 443 && this.port != 8443);
    }

    public boolean testKeystore()
            throws CertificateException, KeyStoreException, NoSuchAlgorithmException, IOException {
        char[]   pwdArray = keyStorePassword.toCharArray();
        KeyStore ks       = KeyStore.getInstance(keyStoreType);
        ks.load(new FileInputStream(keyStore.substring(5)), pwdArray);
        this.validKeystoreConfig = ks.containsAlias(keyAlias);
        return this.validKeystoreConfig;
    }

}
