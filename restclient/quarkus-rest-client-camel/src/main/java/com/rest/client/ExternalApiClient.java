package com.rest.client;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/external")
@RegisterRestClient(configKey = "external-api")
public interface ExternalApiClient {
    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    String status();
}
