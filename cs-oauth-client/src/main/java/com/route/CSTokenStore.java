package com.route;

import jakarta.enterprise.context.ApplicationScoped;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@ApplicationScoped
public class CSTokenStore {

    private static final String TOKEN_FILE = "tokens.json";
    private final ObjectMapper mapper = new ObjectMapper();

    public void saveTokens(Map<String, String> tokens) throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(TOKEN_FILE), tokens);
    }

    public Map<String, String> loadTokens() throws IOException {
        File file = new File(TOKEN_FILE);
        if (!file.exists()) return null;
        return mapper.readValue(file, Map.class);
    }
}
