package io.sapl.server.ce.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

//TODO: Work with multiple application-*.yml-files, remove stack traces
@Service
public class ApplicationYamlHandler {

    private Map<String, Object> yamlMap;
    private String filename;
    @PostConstruct
    public void init() {
        filename = "application.yml";
        yamlMap = readYamlFromRessources(filename);
    }


    private Map<String, Object> readYamlFromRessources(String file) {
        InputStream inputStream = ApplicationYamlHandler.class.getClassLoader().getResourceAsStream(file);
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            return objectMapper.readValue(inputStream, Map.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Object getAt(String path) {
        String[] p = path.split("/");
        Map<String, Object> currentMap = yamlMap;
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

    public void setAt(String path, Object value) {
        String[] p = path.split("/");
        Map<String, Object> currentMap = yamlMap;
        for (int i = 0; i < p.length; i++) {
            String key = p[i];
            if (i == p.length - 1) {
                // Reached the end of the path, set the value
                currentMap.put(key, value);
            } else {
                // Traverse to the next level in the map
                currentMap.computeIfAbsent(key, k -> new HashMap<>());
                currentMap = (Map<String, Object>) currentMap.get(key);
            }
        }
    }

    public void writeYamlToRessources() {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        try {
            String resourcesPath = ApplicationYamlHandler.class.getResource("/").getPath();
            String filePath = resourcesPath + this.filename;
            objectMapper.writeValue(new File(filePath), yamlMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}