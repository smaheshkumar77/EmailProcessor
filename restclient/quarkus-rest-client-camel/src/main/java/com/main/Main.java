package com.main;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.impl.DefaultCamelContext;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

import com.route.CamelClient;


@ApplicationScoped
public class Main {
    //public static void main(String[] args) throws Exception {
      @PostConstruct  
      public void init() {
        try {  
        CamelContext context = new DefaultCamelContext();
        context.addRoutes(new CamelClient(
                System.getProperty("APP_KEY"),
                System.getProperty("APP_SEC"),
                "https://127.0.0.1:8080/callback",
                "tokens.json"
        ));
        context.start();

        ProducerTemplate template = context.createProducerTemplate();
        CamelClient client = context.getRegistry().lookupByNameAndType("CamelClient", CamelClient.class);

        client.updateRefreshToken(template);

        System.out.println("Access Token: " + client.getAccessToken());
        System.out.println("Refresh Token: " + client.getRefreshToken());
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}

