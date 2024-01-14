package dev.hiett.clockmonster.resources.v2;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ResourceContext;
import jakarta.ws.rs.core.MediaType;

@RequestScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Path("/v2")
public class V2Router {

    @Inject
    ResourceContext resourceContext;

    @Path("/job")
    public V2JobResource jobResource() {
        return resourceContext.getResource(V2JobResource.class);
    }
}
