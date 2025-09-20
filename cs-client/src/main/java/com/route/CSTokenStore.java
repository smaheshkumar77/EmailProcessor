package com.route;

import jakarta.enterprise.context.ApplicationScoped;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

@ApplicationScoped
public class CSTokenStore {

    private final ObjectMapper mapper = new ObjectMapper();

    public synchronized void saveTokens(String filePath, Map<String, Object> tokens) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), tokens);
    }

    public synchronized Map<String, Object> loadTokens(String filePath) throws IOException {
        File f = new File(filePath);
        if (!f.exists()) {
            return new HashMap<>();
        }
        return mapper.readValue(f, Map.class);
    }
}

