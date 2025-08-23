package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TagPushEvent extends AbstractEvent {
    @JsonProperty("object_kind")
    private String objectKindRaw = "tag_push";

    private User user;

    private Project project;

    @JsonProperty("ref")
    private String tagReference;

    @Override
    protected EventType getEventType() {
        return EventType.TAG_PUSH;
    }
}
