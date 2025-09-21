package com.mycompany.transform;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Client {
    private final String baseUrl;     // e.g. "https://api.schwab.com/v1"
    private final TokenService tokenService;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public Client(String baseUrl, TokenService tokenManager) {
        this.baseUrl = baseUrl;
        this.tokenService = tokenManager;
        this.httpClient = HttpClient.newBuilder()
                                    .connectTimeout(Duration.ofSeconds(30))
                                    .build();
        this.objectMapper = new ObjectMapper();
    }

    private HttpRequest.Builder withAuthHeaders(HttpRequest.Builder builder) throws Exception {
        String accessToken = tokenService.getAccessToken();
        return builder.header("Authorization", "Bearer " + accessToken)
                      .header("Accept", "application/json");
    }

    private String buildUrlWithParams(String path, Map<String, String> queryParams) {
        if (queryParams == null || queryParams.isEmpty()) {
            return baseUrl + path;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl).append(path).append("?");
        boolean first = true;
        for (Map.Entry<String,String> e : queryParams.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
            sb.append("=");
            sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
        }
        return sb.toString();
    }

    public JsonNode get(String path, Map<String, String> queryParams) throws Exception {
        String url = buildUrlWithParams(path, queryParams);
        HttpRequest request = withAuthHeaders(HttpRequest.newBuilder()
                .uri(new URI(url))
                .GET()
                )
            .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return objectMapper.readTree(resp.body());
        } else {
            throw new ApiException("GET " + url + " failed: " + resp.statusCode() + " - " + resp.body());
        }
    }

    public JsonNode post(String path, Object body) throws Exception {
        String url = baseUrl + path;
        String jsonBody = objectMapper.writeValueAsString(body);
        HttpRequest request = withAuthHeaders(HttpRequest.newBuilder()
                .uri(new URI(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                )
            .build();
        HttpResponse<String> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return objectMapper.readTree(resp.body());
        } else {
            throw new ApiException("POST " + url + " failed: " + resp.statusCode() + " - " + resp.body());
        }
    }

    // Example utility method from client.py
    public JsonNode getPriceHistory(String symbol, int period, String frequencyType, String frequency) throws Exception {
        String path = "/marketdata/v1/pricehistory";
        Map<String,String> params = Map.of(
            "symbol", symbol,
            "period", Integer.toString(period),
            "periodType", "day",
            "frequencyType", frequencyType,
            "frequency", frequency
        );
        return get(path, params);
    }

    // Other methods would follow similarly...

    // Custom exception
    public static class ApiException extends RuntimeException {
        public ApiException(String msg) { super(msg); }
    }
}
