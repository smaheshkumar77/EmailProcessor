package com.route;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;
import java.util.concurrent.CompletionStage;

@Path("/cs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CSResource {

    @Inject
    CSService csService;

    @Inject
    CSConfig config;

    @POST
    @Path("/auth")
    public CompletionStage<Response> startAuth(@QueryParam("code") String code) {
        // Client sends the authorization code here (after user authorizes)
        System.out.println("Received auth code: " + code);
        if (code == null || code.isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "code query param required")).build());
        }

        return csService.postOAuthToken("authorization_code", code)
                .thenApply(map -> Response.ok(map).build())
                .exceptionally(err -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", err.getMessage())).build());
    }

    @POST
    @Path("/refresh")
    public CompletionStage<Response> refresh(@QueryParam("refreshToken") String refreshToken) {
        if (refreshToken == null || refreshToken.isEmpty()) {
            // try auto load from token store
            Map<String, Object> tokens = csService.loadTokensSync();
            Object rt = tokens.get("refresh_token");
            if (rt == null) {
                return java.util.concurrent.CompletableFuture.completedFuture(Response.status(Response.Status.BAD_REQUEST)
                        .entity(Map.of("error", "refreshToken missing and not found in tokens.json")).build());
            }
            refreshToken = rt.toString();
        }

        return csService.postOAuthToken("refresh_token", refreshToken)
                .thenApply(map -> Response.ok(map).build())
                .exceptionally(err -> Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", err.getMessage())).build());
    }
}
