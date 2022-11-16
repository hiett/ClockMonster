package dev.hiett.clockmonster.entities;

public class GenericErrorException extends Exception {

    private final GenericErrorResponse response;

    public GenericErrorException(GenericErrorResponse response) {
        this.response = response;
    }

    public GenericErrorException() {
        this(new GenericErrorResponse());
    }

    public GenericErrorResponse getResponse() {
        return response;
    }
}
