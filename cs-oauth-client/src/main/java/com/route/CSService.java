package com.route;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
public class CSService {

    @Inject
    ProducerTemplate producerTemplate;

    //@PostConstruct
    void init() {
        // Trigger authorization flow manually once
        Map<String, Object> headers = new HashMap<>();
        headers.put("grant_type", "authorization_code");
        headers.put("code", "");
        producerTemplate.sendBody("direct:postOAuthToken", null);
    }

    public void triggerAuthorizationCodeFlow(String authCode) {
        producerTemplate.sendBodyAndHeaders(
            "direct:postOAuthToken",
            null,
            Map.of(
                "grantType", "authorization_code",
                "code", authCode
            )
        );
    }

    public void refreshAccessToken(String refreshToken) {
        producerTemplate.sendBodyAndHeaders(
            "direct:postOAuthToken",
            null,
            Map.of(
                "grantType", "refresh_token",
                "code", refreshToken
            )
        );
    }
}
