package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.Nullable;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Note;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class NoteEvent extends AbstractEvent {
    @JsonProperty("object_kind")
    private String objectKindRaw = "note";

    private User user;

    private Project project;

    private Note note;

    @JsonProperty("merge_request")
    @Nullable
    private MergeRequest mergeRequest;

    @Override
    @JsonIgnore
    protected EventType getEventType() {
        return EventType.NOTE;
    }
}
