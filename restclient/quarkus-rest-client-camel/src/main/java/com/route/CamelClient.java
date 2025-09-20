package com.route;


    

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.JacksonDataFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
/*
public class CamelClient extends RouteBuilder {

    private final String appKey;
    private final String appSecret;
    private final String callbackUrl;
    private final String tokensFile;
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode tokens;

    public CamelClient() {
        // Default no-arg constructor needed by Quarkus/Camel
        this.appKey = System.getProperty("APP_KEY");
        this.appSecret = System.getProperty("APP_SEC");
        this.callbackUrl = "https://127.0.0.1/callback";
        this.tokensFile = "tokens.json";
        try{
        loadTokens();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public CamelClient(String appKey, String appSecret, String callbackUrl, String tokensFile) throws IOException {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.callbackUrl = callbackUrl;
        this.tokensFile = tokensFile;
        try { 
          loadTokens();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // -------------------
    // Load Tokens
    // -------------------
    private void loadTokens() throws IOException {
        File f = new File(tokensFile);
        if (f.exists()) {
            String content = new String(Files.readAllBytes(Paths.get(tokensFile)));
            tokens = mapper.readTree(content);
        }
    }

    private void saveTokens(JsonNode json) throws IOException {
        tokens = json;
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(tokensFile), json);
    }

    // -------------------
    // Configure Camel Routes
    // -------------------
    @Override
    public void configure() {

        JacksonDataFormat jsonFormat = new JacksonDataFormat(JsonNode.class);

        // -------------------
        // Route: Exchange Auth Code or Refresh Token for Access Token
        // -------------------
        from("direct:postOAuthToken")
            .process(exchange -> {
                String grantType = exchange.getIn().getHeader("grantType", String.class);
                String code = exchange.getIn().getHeader("code", String.class);

                String authHeader = "Basic " + Base64.getEncoder()
                        .encodeToString((appKey + ":" + appSecret).getBytes());

                exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                exchange.getIn().setHeader("Authorization", authHeader);
                exchange.getIn().setHeader("Content-Type", "application/x-www-form-urlencoded");

                String body;
                if ("authorization_code".equals(grantType)) {
                    body = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + callbackUrl;
                } else {
                    body = "grant_type=refresh_token&refresh_token=" + code;
                }
                exchange.getIn().setBody(body);
            })
            .to("http://api.schwabapi.com/v1/oauth/token")
            .unmarshal(jsonFormat)
            .process(exchange -> {
                JsonNode responseJson = exchange.getIn().getBody(JsonNode.class);
                saveTokens(responseJson);
            });

        // -------------------
        // Optional: Timer route for refreshing access token periodically
        // -------------------
        from("timer:refreshToken?period=1800000") // every 30 min
            .choice()
                .when(exchange -> tokens != null && tokens.get("refresh_token") != null)
                    .setHeader("grantType", constant("refresh_token"))
                    .setHeader("code", simple("${header.refreshToken}"))
                    .to("direct:postOAuthToken");
    }

    // -------------------
    // Helper to initiate auth flow
    // -------------------
    public void updateRefreshToken(ProducerTemplate template) throws Exception {
        String authUrl = String.format("https://api.schwabapi.com/v1/oauth/authorize?client_id=%s&redirect_uri=%s", appKey, callbackUrl);
        System.out.println("[Schwabdev] Open to authenticate: " + authUrl);

        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(authUrl));
            }
        } catch (Exception e) {
            System.out.println("Open URL manually if browser cannot launch");
        }

        Scanner scanner = new Scanner(System.in);
        System.out.print("[Schwabdev] Paste the URL here: ");
        String responseUrl = scanner.nextLine();

        String code = responseUrl.substring(responseUrl.indexOf("code=") + 5, responseUrl.indexOf("%40")) + "@";

        template.sendBodyAndHeaders("direct:postOAuthToken", null, Map.of(
                "grantType", "authorization_code",
                "code", code
        ));

        System.out.println("Tokens updated and saved to " + tokensFile);
    }

    public String getAccessToken() {
        return tokens != null ? tokens.get("access_token").asText() : null;
    }

    public String getRefreshToken() {
        return tokens != null ? tokens.get("refresh_token").asText() : null;
    }
}
*/