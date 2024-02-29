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
public class ApplicationConfigService {

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
        this.httpEndpoint.setAddress(this.getAt(httpEndpoint.ADDRESS_PATH, "").toString());
        try {
            var port = this.getAt(httpEndpoint.PORT_PATH, "").toString();
            this.httpEndpoint.setPort(getPortNumber(port));
        } catch (NumberFormatException e) {
            this.httpEndpoint.setPort(httpEndpoint.DEFAULT_PORT);
        }
        this.httpEndpoint.setSslEnabled(Boolean.parseBoolean(this.getAt(httpEndpoint.SSL_ENABLED_PATH, "").toString()));

        if (this.httpEndpoint.getSslEnabled()) {
            this.httpEndpoint
                    .setEnabledSslProtocols(this.getAt(httpEndpoint.SSL_ENABLED_PROTOCOLS_PATH, "").toString());
            this.httpEndpoint.setKeyStoreType(this.getAt(httpEndpoint.SSL_KEY_STORE_TYPE_PATH, "").toString());
            this.httpEndpoint.setKeyStore(this.getAt(httpEndpoint.SSL_KEY_STORE_PATH, "").toString());
            this.httpEndpoint.setKeyPassword(this.getAt(httpEndpoint.SSL_KEY_PASSWORD_PATH, "").toString());
            this.httpEndpoint.setKeyStorePassword(this.getAt(httpEndpoint.SSL_KEY_STORE_PASSWORD_PATH, "").toString());
            this.httpEndpoint.setKeyAlias(this.getAt(httpEndpoint.SSL_KEY_ALIAS_PATH, "").toString());
            this.httpEndpoint.setCiphers(this.getAt(httpEndpoint.SSL_CIPHERS_PATH, "").toString());
        }
    }

    private void initRsocketEndpointConfig() {
        this.rsocketEndpoint.setAddress(this.getAt(rsocketEndpoint.ADDRESS_PATH, "").toString());
        try {
            var port = this.getAt(rsocketEndpoint.PORT_PATH, "").toString();
            this.rsocketEndpoint.setPort(getPortNumber(port));
        } catch (NumberFormatException e) {
            this.rsocketEndpoint.setPort(rsocketEndpoint.DEFAULT_PORT);
        }
        this.rsocketEndpoint
                .setSslEnabled(Boolean.parseBoolean(this.getAt(rsocketEndpoint.SSL_ENABLED_PATH, "").toString()));

        if (this.rsocketEndpoint.getSslEnabled()) {
            this.rsocketEndpoint
                    .setEnabledSslProtocols(this.getAt(rsocketEndpoint.SSL_ENABLED_PROTOCOLS_PATH, "").toString());
            this.rsocketEndpoint.setKeyStoreType(this.getAt(rsocketEndpoint.SSL_KEY_STORE_TYPE_PATH, "").toString());
            this.rsocketEndpoint.setKeyStore(this.getAt(rsocketEndpoint.SSL_KEY_STORE_PATH, "").toString());
            this.rsocketEndpoint.setKeyPassword(this.getAt(rsocketEndpoint.SSL_KEY_PASSWORD_PATH, "").toString());
            this.rsocketEndpoint
                    .setKeyStorePassword(this.getAt(rsocketEndpoint.SSL_KEY_STORE_PASSWORD_PATH, "").toString());
            this.rsocketEndpoint.setKeyAlias(this.getAt(rsocketEndpoint.SSL_KEY_ALIAS_PATH, "").toString());
            this.rsocketEndpoint.setCiphers(this.getAt(rsocketEndpoint.SSL_CIPHERS_PATH, "").toString());
        }
    }

    public void persistHttpEndpointConfig() throws IOException {
        this.setAt(httpEndpoint.PORT_PATH, "${PORT:" + this.httpEndpoint.getPort() + "}");
        this.setAt(httpEndpoint.ADDRESS_PATH, httpEndpoint.getAdr());

        boolean tls_enabled = this.httpEndpoint.getSslEnabled();
        this.setAt(httpEndpoint.SSL_ENABLED_PATH, tls_enabled);

        if (tls_enabled) {
            this.setAt(httpEndpoint.SSL_KEY_STORE_TYPE_PATH, this.httpEndpoint.getKeyStoreType());
            this.setAt(httpEndpoint.SSL_KEY_STORE_PATH, this.httpEndpoint.getKeyStore());
            this.setAt(httpEndpoint.SSL_KEY_STORE_PASSWORD_PATH, this.httpEndpoint.getKeyStorePassword());
            this.setAt(httpEndpoint.SSL_KEY_PASSWORD_PATH, this.httpEndpoint.getKeyPassword());
            this.setAt(httpEndpoint.SSL_KEY_ALIAS_PATH, this.httpEndpoint.getKeyAlias());
            this.setAt(httpEndpoint.SSL_CIPHERS_PATH, this.httpEndpoint.getSelectedCiphers());
            this.setAt(httpEndpoint.SSL_ENABLED_PROTOCOLS_PATH,
                    this.httpEndpoint.getEnabledSslProtocols().split(" \\+ "));
            this.setAt(httpEndpoint.SSL_PROTOCOLS_PATH, EndpointConfig.TLS_V1_3_PROTOCOL);
        }

        this.saveYmlFiles();
    }

    public void persistRsocketEndpointConfig() throws IOException {
        this.setAt(rsocketEndpoint.PORT_PATH, "${PORT:" + this.rsocketEndpoint.getPort() + "}");
        this.setAt(rsocketEndpoint.ADDRESS_PATH, rsocketEndpoint.getAdr());
        this.setAt(rsocketEndpoint.TRANSPORT_PATH, "tcp");

        boolean tls_enabled = this.rsocketEndpoint.getSslEnabled();
        this.setAt(rsocketEndpoint.SSL_ENABLED_PATH, tls_enabled);

        if (tls_enabled) {
            this.setAt(rsocketEndpoint.SSL_KEY_STORE_TYPE_PATH, this.rsocketEndpoint.getKeyStoreType());
            this.setAt(rsocketEndpoint.SSL_KEY_STORE_PATH, this.rsocketEndpoint.getKeyStore());
            this.setAt(rsocketEndpoint.SSL_KEY_STORE_PASSWORD_PATH, this.rsocketEndpoint.getKeyStorePassword());
            this.setAt(rsocketEndpoint.SSL_KEY_PASSWORD_PATH, this.rsocketEndpoint.getKeyPassword());
            this.setAt(rsocketEndpoint.SSL_KEY_ALIAS_PATH, this.rsocketEndpoint.getKeyAlias());
            this.setAt(rsocketEndpoint.SSL_CIPHERS_PATH, this.rsocketEndpoint.getSelectedCiphers());
            this.setAt(rsocketEndpoint.SSL_ENABLED_PROTOCOLS_PATH,
                    this.rsocketEndpoint.getEnabledSslProtocols().split(" \\+ "));
            this.setAt(rsocketEndpoint.SSL_PROTOCOLS_PATH, EndpointConfig.TLS_V1_3_PROTOCOL);
        }

        this.saveYmlFiles();
    }

    private int getPortNumber(String s) {
        s.replace("${PORT:", "");
        s.replace("}", "");
        return Integer.parseInt(s);
    }
}
