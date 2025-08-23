package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
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

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    @JsonIgnore
    private String tagName;

    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    @JsonIgnore
    private String url;

    public String getTagName() {
        if (tagName == null) {
            // remove 'refs/tags/' from ref
            String[] parts = tagReference.split("/");
            tagName = parts[parts.length - 1];
        }
        return tagName;
    }

    public String getUrl() {
        if (url == null) {
            url = project.getWebUrl() + "/-/tags/" + getTagName();
        }
        return url;
    }

    @Override
    protected EventType getEventType() {
        return EventType.TAG_PUSH;
    }
}
