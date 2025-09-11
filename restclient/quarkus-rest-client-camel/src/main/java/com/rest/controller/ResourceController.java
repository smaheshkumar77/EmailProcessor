package com.rest.controller;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
