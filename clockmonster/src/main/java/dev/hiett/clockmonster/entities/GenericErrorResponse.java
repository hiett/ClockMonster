package dev.hiett.clockmonster.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.quarkus.runtime.annotations.RegisterForReflection;

import javax.ws.rs.core.Response;

@RegisterForReflection
public class GenericErrorResponse {

    private String message;
    private int status;
    private final boolean error;

    public GenericErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
        this.error = true;
    }

    public GenericErrorResponse() {
        this("Unknown Error", 500);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isError() {
        return error;
    }

    @JsonIgnore
    public Response toResponse() {
        return Response.status(this.status).entity(this).build();
    }
}
