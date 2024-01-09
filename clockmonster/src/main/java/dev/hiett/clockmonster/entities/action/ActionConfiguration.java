package dev.hiett.clockmonster.entities.action;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.hiett.clockmonster.entities.action.impls.HttpActionPayload;
import dev.hiett.clockmonster.entities.action.impls.SqsActionPayload;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.mutiny.sqlclient.Row;
import jakarta.validation.*;

import java.beans.ConstructorProperties;
import java.util.Set;

@RegisterForReflection
public class ActionConfiguration {

    private static final Validator validator;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Valid
    private HttpActionPayload http;

    @Valid
    private SqsActionPayload sqs;

    @ConstructorProperties({"http", "sqs"})
    public ActionConfiguration(HttpActionPayload http, SqsActionPayload sqs) {
        this.http = http;
        this.sqs = sqs;
    }

    public ActionConfiguration() {
        this.http = null;
        this.sqs = null;
    }

    @JsonIgnore
    public ActionType getType() {
        if(http != null)
            return ActionType.HTTP;

        if(sqs != null)
            return ActionType.SQS;

        return null;
    }

    @JsonIgnore
    public ActionPayload getPayload() {
        switch(getType()) {
            case HTTP: return http;
            case SQS: return sqs;
            default: return null;
        }
    }

    public boolean validateActionConfiguration(){
        ActionType type = this.getType();
        if(type == null)
            return false;

        // Validate the relevant payload
        ActionPayload payload = this.getPayload();
        Set<ConstraintViolation<ActionPayload>> validationViolations = validator.validate(payload);

        return validationViolations.isEmpty();
    }

    // Individual type getters for reflection
    public HttpActionPayload getHttp() {
        return http;
    }

    public SqsActionPayload getSqs() {
        return sqs;
    }

    public void setHttp(HttpActionPayload http) {
        this.http = http;
    }

    public void setSqs(SqsActionPayload sqs) {
        this.sqs = sqs;
    }

    // Static //
    public static ActionConfiguration fromRow(Row row) {
        return fromRow(row, "action");
    }

    public static ActionConfiguration fromRow(Row row, String column) {
        String actionString = row.getString(column);
        if(actionString != null) {
            try {
                return objectMapper.readValue(actionString, ActionConfiguration.class);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
