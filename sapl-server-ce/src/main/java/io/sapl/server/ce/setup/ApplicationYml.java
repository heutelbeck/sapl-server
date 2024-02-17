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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
class ApplicationYml {
    private final File          file;
    private Map<String, Object> map;
    private boolean             hasBeenChanged = false;

    ApplicationYml(File f) {
        this.file = f;
    }

    void initMap() throws IOException {

        if (file.exists()) {
            InputStream  inputStream  = new FileInputStream(file);
            ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
            TypeReference<HashMap<String,Object>> typeReference = new TypeReference<>() {
            };
            try {
                map = objectMapper.readValue(inputStream, typeReference);
                return;
            } catch (IOException e) {
                log.warn(file + " is an invalid yml-file. Setup wizard will create an new, empty file");

            }
        } else {
            log.info(file + " does not exist. Setup wizard will create it on save");
        }
        map = new HashMap<>();
    }

    public void saveYamlFile() throws IOException {
        if (!hasBeenChanged) {
            return;
        }
        if (file.getParentFile().mkdirs()) {
            log.info("Created directory " + file.getParent());
        }
        if (file.createNewFile()) {
            log.info("Created file " + file);
        }
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.writeValue(file, map);
    }

    @SuppressWarnings("unchecked")
    public Object getAt(String path) {
        String[]            p          = path.split("/");
        Map<String, Object> currentMap = this.map;
        for (String key : p) {
            if (currentMap.containsKey(key)) {
                Object obj = currentMap.get(key);
                if (obj instanceof Map) {
                    currentMap = (Map<String, Object>) obj;
                } else {
                    return obj;
                }
            } else {
                return null;
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public void setAt(String path, Object value) {
        if (getAt(path) == null || !getAt(path).equals(value)) {
            hasBeenChanged = true;
        }
        String[]            p          = path.split("/");
        Map<String, Object> currentMap = map;
        for (int i = 0; i < p.length; i++) {
            String key = p[i];
            if (i == p.length - 1) {
                // Reached the end of the path, set the value
                currentMap.put(key, value);
            }
            else {
                // Traverse to the next level in the map
                currentMap.computeIfAbsent(key, k -> new HashMap<>());
                currentMap = (Map<String, Object>) currentMap.get(key);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public boolean existsAt(String path) {
        String[]            p          = path.split("/");
        Map<String, Object> currentMap = this.map;
        for (String key : p) {
            if (currentMap.containsKey(key)) {
                Object obj = currentMap.get(key);
                if (obj instanceof Map) {
                    currentMap = (Map<String, Object>) obj;
                } else {
                    return true;
                }
            } else {
                return false;
            }
        }
        return false;
    }

}
