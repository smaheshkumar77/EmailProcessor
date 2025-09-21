package com.mycompany;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.Duration;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TokenManager {
    private final String clientId;
    private final String clientSecret;
    private String refreshToken;
    private String accessToken;
    private Instant accessTokenExpiry;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TokenManager(String clientId, String clientSecret, String refreshToken) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    public synchronized String getAccessToken() throws Exception {
        if (accessToken == null || Instant.now().isAfter(accessTokenExpiry.minus(Duration.ofSeconds(60)))) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() throws Exception {
        String body = "grant_type=refresh_token&refresh_token=" +
                URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI("https://api.schwab.com/oauth/token"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to refresh token: " + response.body());
        }
        TokenResponse tr = objectMapper.readValue(response.body(), TokenResponse.class);
        this.accessToken = tr.access_token();
        this.refreshToken = tr.refresh_token();
        this.accessTokenExpiry = Instant.now().plusSeconds(tr.expires_in());
    }

    public record TokenResponse(String access_token, String refresh_token, long expires_in) {}
}

