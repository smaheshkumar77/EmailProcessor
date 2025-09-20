package com.route;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.client.HttpResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.HashMap;

@ApplicationScoped
public class CSService {

    @Inject
    CSConfig config;

    @Inject
    CSTokenStore tokenStore;

    @Inject
    Vertx vertx; // Quarkus supplies Vertx

    private WebClient webClient;

    @PostConstruct
    void init() {
        webClient = WebClient.create(vertx);
    }

    /**
     * Exchange authorization_code or refresh_token for tokens.
     * grantType must be "authorization_code" or "refresh_token".
     * Returns CompletionStage with the response body parsed into Map.
     */
    public CompletableFuture<Map<String, Object>> postOAuthToken(String grantType, String code) {
        CompletableFuture<Map<String, Object>> cf = new CompletableFuture<>();
        System.out.println("postOAuthToken called with grantType=" + grantType + ", code=" + code);
        if (grantType == null || (!grantType.equals("authorization_code") && !grantType.equals("refresh_token"))) {
            cf.completeExceptionally(new IllegalArgumentException("Invalid grant type"));
            return cf;
        }

        String authHeader = "Basic " + Base64.getEncoder()
                .encodeToString((config.getAppKey() + ":" + config.getAppSecret()).getBytes(StandardCharsets.UTF_8));
        System.out.println("Auth header: " + authHeader);
        // Build form body
        String form;
        if ("authorization_code".equals(grantType)) {
            form = "grant_type=authorization_code&code=" + encode(code) + "&redirect_uri=" + encode(config.getCallbackUrl());
        } else {
            form = "grant_type=refresh_token&refresh_token=" + encode(code);
        }
        System.out.println("Form body: " + form);
        webClient.postAbs("https://api.schwabapi.com/v1/oauth/token")
                .putHeader("Authorization", authHeader)
                .putHeader("Content-Type", "application/x-www-form-urlencoded")
                .as(BodyCodec.jsonObject())
                .sendBuffer(Buffer.buffer(form), ar -> {
                    if (ar.succeeded()) {
                        HttpResponse<io.vertx.core.json.JsonObject> resp = (HttpResponse) ar.result();
                        int status = resp.statusCode();
                        if (status >= 200 && status < 300) {
                            io.vertx.core.json.JsonObject body = resp.body();
                            // convert to Map<String,Object>
                            Map<String, Object> map = body.getMap();
                            // persist tokens
                            try {
                                tokenStore.saveTokens(config.getTokensFile(), map);
                            } catch (Exception e) {
                                // Non-fatal: log & continue
                                e.printStackTrace();
                            }
                            cf.complete(map);
                        } else {
                            cf.completeExceptionally(new RuntimeException("HTTP " + status + ": " + resp.bodyAsString()));
                        }
                    } else {
                        cf.completeExceptionally(ar.cause());
                    }
                });

        return cf;
    }

    private static String encode(String v) {
        if (v == null) return "";
        return java.net.URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    public Map<String, Object> loadTokensSync() {
        try {
            return tokenStore.loadTokens(config.getTokensFile());
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}

