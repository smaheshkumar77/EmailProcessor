package com.main;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.route.CSCamelClient;

@ApplicationScoped
public class CSService {

    @Inject
    CSCamelClient csClient;

    public void init() throws Exception {
        // Start authentication if needed
        if (csClient.getAccessToken() == null) {
            csClient.updateRefreshToken();
        }

        System.out.println("Access Token: " + csClient.getAccessToken());
    }
}
