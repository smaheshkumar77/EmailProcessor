package org.acme;

import io.smallrye.mutiny.Uni;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
public class GreetingResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from Quarkus REST";
    }
    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> helloUni() {
        return Uni.createFrom().item("Hello from Quarkus REST uni");
    }

    @GET
    @Path("/uni")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String>getUni() {
        return Uni.createFrom().item("Hello from Quarkus REST uni");
    }

    @GET
    @Path("/async")
    @Produces(MediaType.TEXT_PLAIN)
    public Uni<String> asyncHello() {
        return Uni.createFrom().item(() -> "Async Hello").onItem().delayIt()
                                            .by(java.time.Duration.ofSeconds(5))
                                        ;
    }

}

    
