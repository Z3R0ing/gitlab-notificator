package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.Nullable;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequestEvent extends AbstractEvent {
    @JsonProperty("object_kind")
    private String objectKindRaw = "merge_request";

    private User user;

    private Project project;

    @JsonProperty("object_attributes")
    private MergeRequest mergeRequest;

    private Changes changes;

    @Nullable
    private List<User> assignees;

    @Nullable
    private List<User> reviewers;

    @Override
    @JsonIgnore
    protected EventType getEventType() {
        return EventType.MERGE_REQUEST;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Changes {
        @Nullable
        private DraftChanges draft;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class DraftChanges {
        private String previous;
        private String current;
    }
}
