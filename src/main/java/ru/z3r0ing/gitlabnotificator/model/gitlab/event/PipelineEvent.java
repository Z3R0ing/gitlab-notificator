package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.lang.Nullable;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Pipeline;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PipelineEvent extends AbstractEvent {
    @JsonProperty("object_kind")
    private String objectKindRaw = "pipeline";

    private User user;

    private Project project;

    private Pipeline pipeline;

    @JsonProperty("merge_request")
    @Nullable
    private MergeRequest mergeRequest;

    @JsonProperty("builds")
    private List<Stages> stages;

    @Override
    @JsonIgnore
    protected EventType getEventType() {
        return EventType.PIPELINE;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Stages {
        private Long id;
        private String stage;
        private String status;
    }
}
