package com.route;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/cs")
public class CSResource {

    @Inject
    CSService csService;

    @POST
    @Path("/auth")
    public String startAuth(@QueryParam("code") String code) {
        csService.triggerAuthorizationCodeFlow(code);
        return "CS authorization code flow triggered";
    }

    @POST
    @Path("/refresh")
    public String refresh(@QueryParam("refreshToken") String refreshToken) {
        csService.refreshAccessToken(refreshToken);
        return "CS refresh token flow triggered";
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello RESTEasy";
    }
}
