package dev.hiett.clockmonster.entities.action;

import io.quarkus.runtime.annotations.RegisterForReflection;
import io.vertx.mutiny.sqlclient.Row;
import org.hibernate.validator.constraints.URL;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@RegisterForReflection
public class ActionConfiguration {

    @NotNull(message = "You must provide an action type!")
    private ActionType type;

    @NotEmpty(message = "You must provide a url!")
    @URL(message = "URL must be a valid URL!")
    private String url;

    public ActionConfiguration(ActionType type, String url) {
        this.type = type;
        this.url = url;
    }

    public ActionConfiguration() {}

    public ActionType getType() {
        return type;
    }

    public void setType(ActionType type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // Static //
    public static ActionConfiguration fromRow(Row row) {
        return new ActionConfiguration(
                ActionType.valueOf(row.getString("action_type")),
                row.getString("action_url")
        );
    }
}
