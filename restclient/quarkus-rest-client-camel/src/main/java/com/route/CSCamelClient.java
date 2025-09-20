package com.route;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.jackson.JacksonDataFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;

@ApplicationScoped
public class CSCamelClient extends RouteBuilder {

    @ConfigProperty(name = "cs.app-key")
    String appKey;

    @ConfigProperty(name = "cs.app-sec")
    String appSecret;

    @ConfigProperty(name = "cs.callback-url")
    String callbackUrl;

    @ConfigProperty(name = "cs.tokens-file")
    String tokensFile;

    @ConfigProperty(name = "cs.refresh-interval")
    long refreshInterval;

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode tokens;

    @Inject
    CamelContext camelContext;

    @PostConstruct
    void init() throws Exception {
        loadTokens();
        camelContext.addRoutes(this);
    }

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

    @Override
    public void configure() throws Exception {
        JacksonDataFormat jsonFormat = new JacksonDataFormat(JsonNode.class);
/*
        rest("/start")
            .post("/auth")
            //.to("direct:postOAuthToken")
            ;
*/
        // OAuth Token Exchange Route
        from("direct:postOAuthToken")
            .process(exchange -> {
                String grantType = exchange.getIn().getHeader("grantType", String.class);
                String code = exchange.getIn().getHeader("code", String.class);

                String authHeader = "Basic " + Base64.getEncoder()
                        .encodeToString((appKey + ":" + appSecret).getBytes());

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
            .process(exchange -> saveTokens(exchange.getIn().getBody(JsonNode.class)));

        // Automatic Token Refresh Route
        from("timer:refreshToken?period=" + refreshInterval)
            .choice()
                .when(exchange -> tokens != null && tokens.get("refresh_token") != null)
                    .setHeader("grantType", constant("refresh_token"))
                    .setHeader("code", simple("${header.refreshToken}"))
                    .to("direct:postOAuthToken");

    }

    public void updateRefreshToken() throws Exception {
        String authUrl = String.format(
                "https://api.schwabapi.com/v1/oauth/authorize?client_id=%s&redirect_uri=%s",
                appKey, callbackUrl);
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

        ProducerTemplate template = camelContext.createProducerTemplate();
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