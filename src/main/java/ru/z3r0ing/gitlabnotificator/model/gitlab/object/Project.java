package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
    private Long id;

    private String name;

    @JsonProperty("web_url")
    private String webUrl;
}
