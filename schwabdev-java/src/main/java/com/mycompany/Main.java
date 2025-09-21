package com.mycompany;

import com.mycompany.TokenManager;
import com.mycompany.SchwabClient;
import com.mycompany.Streamer;

public class Main {
    public static void main(String[] args) {
        try {
            String clientId = System.getenv("CS_APP_KEY");
            String clientSecret = System.getenv("CS_APP_SEC");
            String refreshToken = System.getenv("CS_REFRESH_TOKEN");

            TokenManager tokenManager = new TokenManager(clientId, clientSecret, refreshToken);
            SchwabClient client = new SchwabClient(tokenManager);

            // Example API call
            String accountInfo = client.get("/accounts", String.class);
            System.out.println("Account Info: " + accountInfo);

            // Example streaming
            Streamer streamer = new Streamer();
            streamer.start("wss://stream.schwab.com/v1/marketdata",
                    msg -> System.out.println("Stream: " + msg))
                .join();

            Thread.sleep(5000);
            streamer.stop().join();
            System.out.println("Streaming stopped.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
