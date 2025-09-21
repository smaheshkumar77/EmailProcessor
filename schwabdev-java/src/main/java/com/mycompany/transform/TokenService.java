package com.mycompany.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenService {
    private final String apiKey;
    private final String appSecret;
    private final String callbackUrl;
    private final Path tokenFilePath;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TokenService(String apiKey, String appSecret, String callbackUrl, Path tokenFilePath) {
        this.apiKey = apiKey;
        this.appSecret = appSecret;
        this.callbackUrl = callbackUrl;
        this.tokenFilePath = tokenFilePath;
        this.httpClient = HttpClient.newBuilder()
                                    .version(HttpClient.Version.HTTP_2)
                                    .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Read token from file, if exists.
     */
    public Token readTokenFromFile() throws IOException {
        if (Files.exists(tokenFilePath)) {
            try (InputStream in = Files.newInputStream(tokenFilePath)) {
                return objectMapper.readValue(in, Token.class);
            }
        }
        return null;
    }

    /**
     * Write token to file.
     */
    public void writeTokenToFile(Token token) throws IOException {
        // ensure parent dirs
        Files.createDirectories(tokenFilePath.getParent());
        try (OutputStream out = Files.newOutputStream(tokenFilePath,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            objectMapper.writeValue(out, token);
        }
    }

    /**
     * Check if a token is expired or about to expire.
     */
    public boolean isTokenExpired(Token token) {
        // Python likely computes based on "expires_in" or "obtained_at" timestamp
        Instant now = Instant.now();
        // assume token has a field "expires_at" or compute from "obtained_at + expires_in"
        return token.getExpiresAt().isBefore(now.minusSeconds(60));
    }

    /**
     * Use an OAuth authorization code to fetch access + refresh tokens.
     */
    public Token fetchTokenWithAuthCode(String authCode) throws Exception {
        // Build the token request
        URI uri = new URI("https://api.schwab.com/oauth/token");  // adjust to actual Schwab token endpoint
        var form = Map.of(
            "grant_type", "authorization_code",
            "code", authCode,
            "client_id", apiKey,
            "client_secret", appSecret,
            "redirect_uri", callbackUrl
        );
        String formBody = buildFormData(form);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new TokenException("Error fetching token: " + response.statusCode() + " " + response.body());
        }
        TokenResponse tr = objectMapper.readValue(response.body(), TokenResponse.class);

        Token token = new Token(
            tr.accessToken(),
            tr.refreshToken(),
            Instant.now().plusSeconds(tr.expiresIn()),
            Instant.now()  // obtainedAt
        );
        writeTokenToFile(token);

        return token;
    }

    /**
     * Refresh token using the refresh token.
     */
    public Token refreshToken(Token oldToken) throws Exception {
        URI uri = new URI("https://api.schwab.com/oauth/token");  // adjust to actual endpoint
        var form = Map.of(
            "grant_type", "refresh_token",
            "refresh_token", oldToken.getRefreshToken(),
            "client_id", apiKey,
            "client_secret", appSecret
        );
        String formBody = buildFormData(form);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(uri)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new TokenException("Error refreshing token: " + response.statusCode() + " " + response.body());
        }
        TokenResponse tr = objectMapper.readValue(response.body(), TokenResponse.class);

        Token newToken = new Token(
            tr.accessToken(),
            tr.refreshToken(),
            Instant.now().plusSeconds(tr.expiresIn()),
            Instant.now()
        );
        writeTokenToFile(newToken);

        return newToken;
    }

    private String buildFormData(Map<String, String> data) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : data.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(encode(e.getKey()));
            sb.append("=");
            sb.append(encode(e.getValue()));
        }
        return sb.toString();
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    // Data classes

    public static class Token {
        private String accessToken;
        private String refreshToken;
        private Instant expiresAt;      // absolute time when this token expires
        private Instant obtainedAt;     // when we fetched this token

        public Token() { }

        public Token(String accessToken, String refreshToken, Instant expiresAt, Instant obtainedAt) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
            this.obtainedAt = obtainedAt;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public Instant getExpiresAt() { return expiresAt; }
        public Instant getObtainedAt() { return obtainedAt; }

        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
        public void setObtainedAt(Instant obtainedAt) { this.obtainedAt = obtainedAt; }
    }

    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_in")
        private long expiresIn;

        public TokenResponse() { }

        public String accessToken() { return accessToken; }
        public String refreshToken() { return refreshToken; }
        public long expiresIn() { return expiresIn; }
    }

    public static class TokenException extends RuntimeException {
        public TokenException(String message) { super(message); }
    }
}

