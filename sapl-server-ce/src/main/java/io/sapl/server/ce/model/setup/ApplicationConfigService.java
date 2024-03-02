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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Conditional(SetupNotFinishedCondition.class)
@Slf4j
public class ApplicationConfigService {

    private static final String           PORT_PREFIX     = "${PORT:";
    private final List<ApplicationYml>    appYmls         = new ArrayList<>();
    private final ConfigurableEnvironment env;
    @Getter
    private final DBMSConfig              dbmsConfig      = new DBMSConfig();
    @Getter
    private final AdminUserConfig         adminUserConfig = new AdminUserConfig();
    @Getter
    private final EndpointConfig          httpEndpoint    = new EndpointConfig("server.", 8443);
    @Getter
    final EndpointConfig                  rsocketEndpoint = new EndpointConfig("spring.rsocket.server.", 7000);

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

    public void saveYmlFiles() throws IOException {
        for (ApplicationYml f : appYmls) {
            f.saveYmlFile();
        }
    }

    public void initDbmsConfig() {
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
        this.saveYmlFiles();
    }

    public void initAdminUserConfig() {
        this.adminUserConfig.setUsername(this.getAt(AdminUserConfig.USERNAME_PATH, "").toString());
    }

    public void persistAdminUserConfig() throws IOException {
        this.setAt(AdminUserConfig.USERNAME_PATH, this.adminUserConfig.getUsername());
        this.setAt(AdminUserConfig.ENCODEDPASSWORD_PATH, this.adminUserConfig.getEncodedPassword());
        this.saveYmlFiles();
    }

    private void initHttpEndpointConfig() {
        this.httpEndpoint.setAddress(this.getAt(httpEndpoint.addressPath, "localhost").toString());
        try {
            var port = this.getAt(httpEndpoint.portPath, "").toString();
            this.httpEndpoint.setPort(getPortNumber(port));
        } catch (NumberFormatException e) {
            log.warn("Can't retrieve RSocket port from properties. Falling back to port " + this.httpEndpoint.getPort());

        }
        this.httpEndpoint
                .setSslEnabled(Boolean.parseBoolean(this.getAt(httpEndpoint.sslEnabledPath, "false").toString()));

        if (Boolean.TRUE.equals(this.httpEndpoint.getSslEnabled())) {
            this.httpEndpoint
                    .setEnabledSslProtocols(this.getAt(httpEndpoint.sslEnabledProtocolsPath, "").toString());
            this.httpEndpoint.setKeyStoreType(this.getAt(httpEndpoint.sslKeyStoreTypePath, "").toString());
            this.httpEndpoint
                    .setKeyStore(this.getAt(httpEndpoint.sslKeyStorePath, "file:config/keystore.p12").toString());
            this.httpEndpoint.setKeyPassword(this.getAt(httpEndpoint.sslKeyPasswordPath, "").toString());
            this.httpEndpoint.setKeyStorePassword(this.getAt(httpEndpoint.sslKeyStorePasswordPath, "").toString());
            this.httpEndpoint.setKeyAlias(this.getAt(httpEndpoint.sslKeyAliasPath, "").toString());
            this.httpEndpoint.setCiphers(this.getAt(httpEndpoint.sslCiphersPath, "").toString());
        }
    }

    private void initRsocketEndpointConfig() {
        this.rsocketEndpoint.setAddress(this.getAt(rsocketEndpoint.addressPath, "localhost").toString());
        try {
            var port = this.getAt(rsocketEndpoint.portPath, "").toString();
            this.rsocketEndpoint.setPort(getPortNumber(port));
        } catch (NumberFormatException e) {
            log.warn("Can't retrieve RSocket port from properties. Falling back to port " + this.rsocketEndpoint.getPort());
        }
        this.rsocketEndpoint
                .setSslEnabled(Boolean.parseBoolean(this.getAt(rsocketEndpoint.sslEnabledPath, "false").toString()));

        if (Boolean.TRUE.equals(this.rsocketEndpoint.getSslEnabled())) {
            this.rsocketEndpoint
                    .setEnabledSslProtocols(this.getAt(rsocketEndpoint.sslEnabledProtocolsPath, "").toString());
            this.rsocketEndpoint.setKeyStoreType(this.getAt(rsocketEndpoint.sslKeyStoreTypePath, "").toString());
            this.rsocketEndpoint
                    .setKeyStore(this.getAt(rsocketEndpoint.sslKeyStorePath, "file:config/keystore.p12").toString());
            this.rsocketEndpoint.setKeyPassword(this.getAt(rsocketEndpoint.sslKeyPasswordPath, "").toString());
            this.rsocketEndpoint
                    .setKeyStorePassword(this.getAt(rsocketEndpoint.sslKeyStorePasswordPath, "").toString());
            this.rsocketEndpoint.setKeyAlias(this.getAt(rsocketEndpoint.sslKeyAliasPath, "").toString());
            this.rsocketEndpoint.setCiphers(this.getAt(rsocketEndpoint.sslCiphersPath, "").toString());
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
            this.setAt(httpEndpoint.sslCiphersPath, this.httpEndpoint.getSelectedCiphers());
            this.setAt(httpEndpoint.sslEnabledProtocolsPath,
                    this.httpEndpoint.getEnabledSslProtocols().split(" \\+ "));
            this.setAt(httpEndpoint.sslProtocolsPath, EndpointConfig.TLS_V1_3_PROTOCOL);
        }

        this.saveYmlFiles();
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
            this.setAt(rsocketEndpoint.sslCiphersPath, this.rsocketEndpoint.getSelectedCiphers());
            this.setAt(rsocketEndpoint.sslEnabledProtocolsPath,
                    this.rsocketEndpoint.getEnabledSslProtocols().split(" \\+ "));
            this.setAt(rsocketEndpoint.sslProtocolsPath, EndpointConfig.TLS_V1_3_PROTOCOL);
        }

        this.saveYmlFiles();
    }

    private int getPortNumber(String s) {
        s = s.replace(PORT_PREFIX, "").replace("}", "");
        return Integer.parseInt(s);
    }
}
