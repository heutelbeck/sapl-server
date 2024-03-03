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

import io.sapl.server.ce.model.setup.condition.SetupNotFinishedCondition;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Conditional(SetupNotFinishedCondition.class)
@Slf4j
public class ApplicationConfigService {

    private static final String           PORT_PREFIX            = "${PORT:";
    private final List<ApplicationYml>    appYmls                = new ArrayList<>();
    private final ConfigurableEnvironment env;
    @Getter
    private final DBMSConfig              dbmsConfig             = new DBMSConfig();
    @Getter
    private final AdminUserConfig         adminUserConfig        = new AdminUserConfig();
    @Getter
    private final EndpointConfig          httpEndpoint           = new EndpointConfig("server.", 8443);
    @Getter
    private final EndpointConfig          rsocketEndpoint        = new EndpointConfig("spring.rsocket.server.", 7000);
    @Getter
    private final ApiAuthenticationConfig apiAuthenticationConfig = new ApiAuthenticationConfig();

    public ApplicationConfigService(ConfigurableEnvironment env) throws IOException {
        this.env = env;
        for (File f : this.getAppYmlsFromProperties()) {
            var ay = new ApplicationYml(f);
            ay.initMap();
            appYmls.add(ay);
        }
        this.initDbmsConfig();
        this.initAdminUserConfig();
        this.initHttpEndpointConfig();
        this.initRsocketEndpointConfig();
        this.initApiAuthenticationConfig();
    }

    private List<File> getAppYmlsFromProperties() {
        List<File> files = new ArrayList<>();

        String                 projectPath     = System.getProperty("user.dir");
        MutablePropertySources propertySources = this.env.getPropertySources();

        for (PropertySource<?> source : propertySources) {
            String sourceName = source.getName();

            // Get the application.yml-propertySource, but only if it is not located in
            // classpath
            if (sourceName.contains("Config resource") && !sourceName.contains("classpath")) {
                Pattern pattern = Pattern.compile("\\[(.*?)]");
                Matcher matcher = pattern.matcher(sourceName);
                if (matcher.find()) {
                    String filePath = matcher.group(1);
                    if (Paths.get(filePath).isAbsolute()) {
                        files.add(new File(filePath));
                    } else {
                        files.add(new File(projectPath + File.separator + filePath));
                    }
                }
            }
        }
        if (files.isEmpty()) {
            files.add(new File(projectPath + File.separator + "config" + File.separator + "application.yml"));
        }
        return files;
    }

    public Object getAt(String path) {
        for (ApplicationYml f : appYmls) {
            if (f.existsAt(path)) {
                return f.getAt(path);
            }
        }
        return null;
    }

    public Object getAt(String path, Object defaultValue) {
        return ObjectUtils.firstNonNull(this.getAt(path), defaultValue);
    }

    public void setAt(String path, Object value) {
        for (ApplicationYml f : appYmls) {
            if (f.existsAt(path)) {
                f.setAt(path, value);
                return;
            }
        }
        // If value was not found in any file, set it in primary file
        appYmls.get(0).setAt(path, value);
    }

    public void persistYmlFiles() throws IOException {
        for (ApplicationYml f : appYmls) {
            f.persistYmlFile();
        }
    }

    private void initDbmsConfig() {
        this.dbmsConfig.setDbms(DBMSConfig.getDatasourceTypeFromDriverClassName(
                this.getAt(DBMSConfig.DRIVERCLASSNAME_PATH, DBMSConfig.DRIVERCLASSNAME_H2).toString()));
        this.dbmsConfig.setUrl(this.getAt(DBMSConfig.URL_PATH, "").toString());
        if (this.dbmsConfig.getUrl().isEmpty()) {
            dbmsConfig.setToDbmsDefaults(dbmsConfig.getDbms());
        }
        this.dbmsConfig.setUsername(this.getAt(DBMSConfig.USERNAME_PATH, "").toString());
        this.dbmsConfig.setPassword(this.getAt(DBMSConfig.PASSWORD_PATH, "").toString());

    }

    public void persistDbmsConfig() throws IOException {
        this.setAt(DBMSConfig.DRIVERCLASSNAME_PATH, dbmsConfig.getDriverClassName());
        this.setAt(DBMSConfig.URL_PATH, dbmsConfig.getUrl());
        this.setAt(DBMSConfig.USERNAME_PATH, dbmsConfig.getUsername());
        this.setAt(DBMSConfig.PASSWORD_PATH, dbmsConfig.getPassword());
        this.persistYmlFiles();
    }

    private void initAdminUserConfig() {
        this.adminUserConfig.setUsername(this.getAt(AdminUserConfig.USERNAME_PATH, "").toString());
    }

    public void persistAdminUserConfig() throws IOException {
        this.setAt(AdminUserConfig.USERNAME_PATH, this.adminUserConfig.getUsername());
        this.setAt(AdminUserConfig.ENCODEDPASSWORD_PATH, this.adminUserConfig.getEncodedPassword());
        this.persistYmlFiles();
    }

    @SuppressWarnings("unchecked")
    private void initHttpEndpointConfig() {
        this.httpEndpoint.setAddress(this.getAt(httpEndpoint.addressPath, "localhost").toString());

        var port = this.getAt(httpEndpoint.portPath, "").toString();
        if (getPortNumber(port) > 0) {
            this.httpEndpoint.setPort(getPortNumber(port));
        }

        if (Boolean.parseBoolean(this.getAt(httpEndpoint.sslEnabledPath, "false").toString())) {
            if (this.getAt(httpEndpoint.sslEnabledProtocolsPath) != null) {
                if (this.getAt(httpEndpoint.sslEnabledProtocolsPath) instanceof List) {
                    this.httpEndpoint.setEnabledSslProtocols(
                            new HashSet<>(((List<String>) this.getAt(httpEndpoint.sslEnabledProtocolsPath))));
                } else if (this.getAt(httpEndpoint.sslEnabledProtocolsPath) instanceof String string) {
                    this.httpEndpoint
                            .setEnabledSslProtocols(Arrays.stream(string.split(",")).collect(Collectors.toSet()));
                }
            }
            this.httpEndpoint.setKeyStoreType(this.getAt(httpEndpoint.sslKeyStoreTypePath, "").toString());
            this.httpEndpoint
                    .setKeyStore(this.getAt(httpEndpoint.sslKeyStorePath, "file:config/keystore.p12").toString());
            this.httpEndpoint.setKeyPassword(this.getAt(httpEndpoint.sslKeyPasswordPath, "").toString());
            this.httpEndpoint.setKeyStorePassword(this.getAt(httpEndpoint.sslKeyStorePasswordPath, "").toString());
            this.httpEndpoint.setKeyAlias(this.getAt(httpEndpoint.sslKeyAliasPath, "").toString());

            if (this.getAt(httpEndpoint.sslCiphersPath) != null) {
                if (this.getAt(httpEndpoint.sslCiphersPath) instanceof List) {
                    this.httpEndpoint.setCiphers(((List<String>) this.getAt(httpEndpoint.sslCiphersPath)).stream()
                            .map(SupportedCiphers::valueOf).collect(Collectors.toSet()));
                } else if (this.getAt(httpEndpoint.sslCiphersPath) instanceof String string) {
                    this.httpEndpoint.setCiphers(Arrays.stream(string.split(",")).map(SupportedCiphers::valueOf)
                            .collect(Collectors.toSet()));
                }
            }
        }
    }

    public void persistHttpEndpointConfig() throws IOException {
        this.setAt(httpEndpoint.portPath, PORT_PREFIX + this.httpEndpoint.getPort() + "}");
        this.setAt(httpEndpoint.addressPath, httpEndpoint.getAddress());

        boolean tlsEnabled = this.httpEndpoint.getSslEnabled();
        this.setAt(httpEndpoint.sslEnabledPath, tlsEnabled);

        if (tlsEnabled) {
            this.setAt(httpEndpoint.sslKeyStoreTypePath, this.httpEndpoint.getKeyStoreType());
            this.setAt(httpEndpoint.sslKeyStorePath, this.httpEndpoint.getKeyStore());
            this.setAt(httpEndpoint.sslKeyStorePasswordPath, this.httpEndpoint.getKeyStorePassword());
            this.setAt(httpEndpoint.sslKeyPasswordPath, this.httpEndpoint.getKeyPassword());
            this.setAt(httpEndpoint.sslKeyAliasPath, this.httpEndpoint.getKeyAlias());
            this.setAt(httpEndpoint.sslCiphersPath, this.httpEndpoint.getCiphers());
            this.setAt(httpEndpoint.sslEnabledProtocolsPath, this.httpEndpoint.getEnabledSslProtocols());
            this.setAt(httpEndpoint.sslProtocolPath, httpEndpoint.getPrimarySslProtocol());
        }
        this.persistYmlFiles();
    }

    @SuppressWarnings("unchecked")
    private void initRsocketEndpointConfig() {
        this.rsocketEndpoint.setAddress(this.getAt(rsocketEndpoint.addressPath, "localhost").toString());

        var port = this.getAt(rsocketEndpoint.portPath, "").toString();
        if (getPortNumber(port) > 0) {
            this.rsocketEndpoint.setPort(getPortNumber(port));
        }

        if (Boolean.parseBoolean(this.getAt(rsocketEndpoint.sslEnabledPath, "false").toString())) {
            if (this.getAt(rsocketEndpoint.sslEnabledProtocolsPath) != null) {
                if (this.getAt(rsocketEndpoint.sslEnabledProtocolsPath) instanceof List) {
                    this.rsocketEndpoint.setEnabledSslProtocols(
                            new HashSet<>(((List<String>) this.getAt(rsocketEndpoint.sslEnabledProtocolsPath))));
                } else if (this.getAt(rsocketEndpoint.sslEnabledProtocolsPath) instanceof String string) {
                    this.rsocketEndpoint
                            .setEnabledSslProtocols(Arrays.stream(string.split(",")).collect(Collectors.toSet()));
                }
            }
            this.rsocketEndpoint.setKeyStoreType(this.getAt(rsocketEndpoint.sslKeyStoreTypePath, "").toString());
            this.rsocketEndpoint
                    .setKeyStore(this.getAt(rsocketEndpoint.sslKeyStorePath, "file:config/keystore.p12").toString());
            this.rsocketEndpoint.setKeyPassword(this.getAt(rsocketEndpoint.sslKeyPasswordPath, "").toString());
            this.rsocketEndpoint
                    .setKeyStorePassword(this.getAt(rsocketEndpoint.sslKeyStorePasswordPath, "").toString());
            this.rsocketEndpoint.setKeyAlias(this.getAt(rsocketEndpoint.sslKeyAliasPath, "").toString());
            if (this.getAt(rsocketEndpoint.sslCiphersPath) != null) {
                if (this.getAt(rsocketEndpoint.sslCiphersPath) instanceof List) {
                    this.rsocketEndpoint.setCiphers(((List<String>) this.getAt(rsocketEndpoint.sslCiphersPath)).stream()
                            .map(SupportedCiphers::valueOf).collect(Collectors.toSet()));
                } else if (this.getAt(rsocketEndpoint.sslCiphersPath) instanceof String string) {
                    this.rsocketEndpoint.setCiphers(Arrays.stream(string.split(",")).map(SupportedCiphers::valueOf)
                            .collect(Collectors.toSet()));
                }
            }
        }
    }

    public void persistRsocketEndpointConfig() throws IOException {
        this.setAt(rsocketEndpoint.portPath, PORT_PREFIX + this.rsocketEndpoint.getPort() + "}");
        this.setAt(rsocketEndpoint.addressPath, rsocketEndpoint.getAddress());
        this.setAt(rsocketEndpoint.transportPath, "tcp");

        boolean tlsEnabled = this.rsocketEndpoint.getSslEnabled();
        this.setAt(rsocketEndpoint.sslEnabledPath, tlsEnabled);

        if (tlsEnabled) {
            this.setAt(rsocketEndpoint.sslKeyStoreTypePath, this.rsocketEndpoint.getKeyStoreType());
            this.setAt(rsocketEndpoint.sslKeyStorePath, this.rsocketEndpoint.getKeyStore());
            this.setAt(rsocketEndpoint.sslKeyStorePasswordPath, this.rsocketEndpoint.getKeyStorePassword());
            this.setAt(rsocketEndpoint.sslKeyPasswordPath, this.rsocketEndpoint.getKeyPassword());
            this.setAt(rsocketEndpoint.sslKeyAliasPath, this.rsocketEndpoint.getKeyAlias());
            this.setAt(rsocketEndpoint.sslCiphersPath, this.rsocketEndpoint.getCiphers());
            this.setAt(rsocketEndpoint.sslEnabledProtocolsPath, this.rsocketEndpoint.getEnabledSslProtocols());
            this.setAt(rsocketEndpoint.sslProtocolPath, rsocketEndpoint.getPrimarySslProtocol());
        }
        this.persistYmlFiles();
    }

    private void initApiAuthenticationConfig() {
        this.apiAuthenticationConfig.setBasicAuthEnabled(
                Boolean.parseBoolean(this.getAt(ApiAuthenticationConfig.BASICAUTH_PATH, false).toString()));
        this.apiAuthenticationConfig.setApiKeyAuthEnabled(
                Boolean.parseBoolean(this.getAt(ApiAuthenticationConfig.APIKEYAUTH_PATH, false).toString()));
        this.apiAuthenticationConfig
                .setApiKeyHeaderName(this.getAt(ApiAuthenticationConfig.APIKEYHEADERNAME_PATH, "").toString());
        this.apiAuthenticationConfig.setApiKeyCachingEnabled(
                Boolean.parseBoolean(this.getAt(ApiAuthenticationConfig.APIKEYCACHINGENABLED_PATH, false).toString()));
        this.apiAuthenticationConfig.setApiKeyCachingExpires(
                Integer.parseInt(this.getAt(ApiAuthenticationConfig.APIKEYCACHINGEXPIRE_PATH, 0).toString()));
        this.apiAuthenticationConfig.setApiKeyCachingMaxSize(
                Integer.parseInt(this.getAt(ApiAuthenticationConfig.APIKEYCACHINGMAXSIZE_PATH, 0).toString()));
    }

    public void persistApiAuthenticationConfig() throws IOException {
        this.setAt(ApiAuthenticationConfig.BASICAUTH_PATH, this.apiAuthenticationConfig.isBasicAuthEnabled());
        this.setAt(ApiAuthenticationConfig.APIKEYAUTH_PATH, this.apiAuthenticationConfig.isApiKeyAuthEnabled());
        this.setAt(ApiAuthenticationConfig.APIKEYHEADERNAME_PATH, this.apiAuthenticationConfig.getApiKeyHeaderName());
        this.setAt(ApiAuthenticationConfig.APIKEYCACHINGENABLED_PATH,
                this.apiAuthenticationConfig.isApiKeyCachingEnabled());
        this.setAt(ApiAuthenticationConfig.APIKEYCACHINGEXPIRE_PATH,
                this.apiAuthenticationConfig.getApiKeyCachingExpires());
        this.setAt(ApiAuthenticationConfig.APIKEYCACHINGMAXSIZE_PATH,
                this.apiAuthenticationConfig.getApiKeyCachingMaxSize());
        this.persistYmlFiles();
    }

    private int getPortNumber(String s) {
        String  regex   = "\\{PORT:(\\d+)}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            return 0;
        }
    }
}
