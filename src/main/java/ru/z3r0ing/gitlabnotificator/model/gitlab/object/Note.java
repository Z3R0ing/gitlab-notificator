package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Note {
    @JsonProperty("noteable_type")
    private String noteableType;

    private String url;

    private User author;
}
