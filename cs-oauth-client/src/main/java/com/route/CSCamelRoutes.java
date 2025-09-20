package com.route;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Base64;
import java.util.Map;

@ApplicationScoped
public class CSCamelRoutes extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        from("direct:postOAuthToken")
            .log("Triggering CS OAuth route")
            .process(exchange -> {
                String grantType = exchange.getIn().getHeader("grant_type", "authorization_code", String.class);
               
                String code = exchange.getIn().getHeader("code", String.class);

                String appKey = System.getenv("CS_APP_KEY");
                String appSec = System.getenv("CS_APP_SEC");
                String callbackUrl = System.getenv("CS_CALLBACK_URL");

                if (appKey == null || appSec == null) {
                    throw new RuntimeException("CS_APP_KEY or CS_APP_SEC not set in environment");
                }

                String authHeader = "Basic " + Base64.getEncoder()
                        .encodeToString((appKey + ":" + appSec).getBytes());

                String body;
                if ("authorization_code".equals(grantType)) {
                    body = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + callbackUrl;
                } else if ("refresh_token".equals(grantType)) {
                    body = "grant_type=refresh_token&refresh_token=" + code;
                } else {
                    throw new RuntimeException("Invalid grant type: " + grantType);
                }

                exchange.getIn().setHeader("Authorization", authHeader);
                exchange.getIn().setHeader("Content-Type", "application/x-www-form-urlencoded");
                exchange.getIn().setBody(body);

            })
            .to("http://api.schwabapi.com/v1/oauth/token") // CS uses Schwab API endpoint
            .process(exchange -> {
                String response = exchange.getMessage().getBody(String.class);
                System.out.println("[CS OAuth] Response: " + response);

                // Optionally parse JSON and save tokens
                ObjectMapper mapper = new ObjectMapper();
                Map<String, String> tokenMap = mapper.readValue(response, Map.class);

                CSTokenStore tokenStore = new CSTokenStore();
                tokenStore.saveTokens(tokenMap);
            });

        // Timer route to refresh token every 30 minutes
        from("timer://refreshToken?period=1800000")
            .process(exchange -> {
                CSTokenStore tokenStore = new CSTokenStore();
                Map<String, String> tokens = tokenStore.loadTokens();
                if (tokens != null && tokens.containsKey("refresh_token")) {
                    exchange.getIn().setHeader("grantType", "refresh_token");
                    exchange.getIn().setHeader("code", tokens.get("refresh_token"));
                } else {
                    //throw new RuntimeException("No refresh token found for automatic refresh");
                    log.warn("No refresh token found. Skipping automatic refresh until initial auth is completed.");
                    exchange.setProperty(Exchange.ROUTE_STOP, true); // gracefully stop processing 
                }
            })
            .to("direct:postOAuthToken")
            .log("Refreshed access token");
    }
}
