package com.rest.controller;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/jobs")
public class IncomingResource {

    @Inject
    EventBus eventBus; // Mutiny wrapper for Vert.x EventBus

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<String> createJob(String payload) {
        // send message on event bus (non-blocking)
        return eventBus.request("jobs.process", payload)
                      .onItem().transform(msg -> msg.body().toString());
    }
}
