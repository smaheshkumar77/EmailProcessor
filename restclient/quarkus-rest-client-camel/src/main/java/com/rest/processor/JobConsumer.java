package com.rest.processor;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.eventbus.EventBus;
import io.vertx.mutiny.core.eventbus.Message;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.camel.ProducerTemplate;

@ApplicationScoped
public class JobConsumer {

    @Inject
    ProducerTemplate producerTemplate; // Camel producer for direct: routes

    // This method is registered programmatically during startup
    public void register(io.vertx.core.Vertx vertx) {
        vertx.eventBus().consumer("jobs.process", (io.vertx.core.eventbus.Message<Object> m) -> {
            // offload to a worker (don't block event-loop)
            vertx.executeBlocking(promise -> {
                try {
                    // send to camel route 'direct:processJob' and get response synchronously
                    Object result = producerTemplate.requestBody("direct:processJob", m.body());
                    promise.complete(result);
                } catch (Exception e) {
                    promise.fail(e);
                }
            }, res -> {
                if (res.succeeded()) {
                    m.reply(res.result());
                } else {
                    m.fail(500, res.cause().getMessage());
                }
            });
        });
    }
}
