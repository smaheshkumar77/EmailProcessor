package com.rest.router;

import org.apache.camel.builder.RouteBuilder;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@RegisterForReflection
public class RestRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        rest("/test")
            .post("/info")
            .routeId("test")
            .to("direct:processJob")
            ;
  System.out.println("DEBUG :: RestRoute :: rest :: ${body}");

        from("direct:processJob")
            .routeId("process-job")
            // transform/process headers/body as needed
            .log("Processing job: ${body}")
            // call external REST using camel-http or camel-vertx-http
            //.to("vertx-http://api.example.com/external/status")
            .log("External returned: ${body}");
    }
}
