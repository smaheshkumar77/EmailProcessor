package com.mycompany;

import java.net.URI;
import java.net.http.*;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SchwabClient {
    private final TokenManager tokenManager;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String baseUrl = "https://api.schwab.com/v1";

    public SchwabClient(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public <T> T get(String path, Class<T> responseType) throws Exception {
        String token = tokenManager.getAccessToken();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(baseUrl + path))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            return objectMapper.readValue(response.body(), responseType);
        } else {
            throw new RuntimeException("Request failed: " + response.statusCode() + " - " + response.body());
        }
    }
}
