package com.route;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import io.quarkus.scheduler.Scheduled;

import java.util.Map;

@ApplicationScoped
public class AutoRefreshScheduler {

    @Inject
    CSService csService;

    @Inject
    CSConfig config;

    // Runs every configured milliseconds; use fixedDelay to avoid overlapping if a refresh takes long.
    @Scheduled(every = "{cs.refresh.interval.ms}")
    void refreshIfPossible() {
        Map<String, Object> tokens = csService.loadTokensSync();
        if (tokens != null && tokens.containsKey("refresh_token")) {
            String refreshToken = tokens.get("refresh_token").toString();
            csService.postOAuthToken("refresh_token", refreshToken)
                .whenComplete((map, err) -> {
                    if (err != null) {
                        System.err.println("Auto refresh failed: " + err.getMessage());
                    } else {
                        System.out.println("Auto refresh successful");
                    }
                });
        } else {
            // No refresh token yet; skip quietly
            System.out.println("No refresh token available - skipping auto refresh");
        }
    }
}
