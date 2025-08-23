package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {
    private Long id;

    private Long iid;

    private String title;

    private String state;

    private String action;

    private String url;

    private User assignee;
}
