package com.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/external")
@RegisterRestClient(configKey = "external-api")
public interface ExternalApiClient {
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    String status();
}
