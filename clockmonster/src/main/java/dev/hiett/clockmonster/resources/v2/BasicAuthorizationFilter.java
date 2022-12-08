package dev.hiett.clockmonster.resources.v2;

import dev.hiett.clockmonster.entities.GenericErrorResponse;
import io.vertx.core.http.HttpServerRequest;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.Provider;

@Provider
public class BasicAuthorizationFilter implements ContainerRequestFilter {

    private static final String DISABLED_AUTH_PASSWORD = "__disabled__";

    @Context
    UriInfo info;

    @Context
    HttpServerRequest request;

    @ConfigProperty(name = "clockmonster.authorization.basic-auth-value")
    String authPassword;

    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        if (authPassword.equals(DISABLED_AUTH_PASSWORD))
            return; // Auth is disabled

        String authCode = containerRequestContext.getHeaders().getFirst("authorization");
        if (authCode == null || !authCode.equals("Bearer " + authPassword))
            containerRequestContext.abortWith(new GenericErrorResponse("Invalid authorization header.",
                    401).toResponse());
    }
}
