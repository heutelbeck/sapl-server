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

package io.sapl.server.ce.setup;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
@Component
public class ApplicationYamlHandler {

    private final List<ApplicationYml>    appYamls = new ArrayList<>();
    private final ConfigurableEnvironment env;

    public ApplicationYamlHandler(ConfigurableEnvironment env) throws IOException {
        this.env = env;
        for (File f : this.getAppYamlsFromProperties()) {
            var ay = new ApplicationYml(f);
            ay.initMap();
            appYamls.add(ay);

        }
    }

    private List<File> getAppYamlsFromProperties() {
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
        for (ApplicationYml f : appYamls) {
            if(f.existsAt(path)){
                return f.getAt(path);
            }
        }
        return null;
    }

    public Object getAt(String path, Object defaultValue) {
        return ObjectUtils.firstNonNull(this.getAt(path), defaultValue);
    }

    public void setAt(String path, Object value) {
        for (ApplicationYml f : appYamls) {
            if (f.existsAt(path)) {
                f.setAt(path, value);
                return;
            }
        }
        // If value was not found in any file, set it in primary file
        appYamls.get(0).setAt(path, value);
    }

    public void saveYamlFiles() throws IOException {
        for (ApplicationYml f : appYamls) {
            f.saveYamlFile();
        }
    }

}
