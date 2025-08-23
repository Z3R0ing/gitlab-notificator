package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Issue;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueEvent extends AbstractEvent {
    @JsonProperty("object_kind")
    private String objectKindRaw = "issue";

    private User user;

    private Project project;

    @JsonProperty("object_attributes")
    private Issue issue;

    @Override
    @JsonIgnore
    protected EventType getEventType() {
        return EventType.ISSUE;
    }
}
