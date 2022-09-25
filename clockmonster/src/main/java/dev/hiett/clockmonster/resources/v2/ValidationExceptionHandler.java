package dev.hiett.clockmonster.resources.v2;

import dev.hiett.clockmonster.entities.GenericErrorResponse;

import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException e) {
        String errorMsg = e.getConstraintViolations().stream()
                .map(entry -> entry.getPropertyPath().toString() + ": " + entry.getMessage())
                .collect(Collectors.joining(", "));

        // Turn this into a generic error
        return new GenericErrorResponse("Validation errors: " + errorMsg, 400).toResponse();
    }
}
