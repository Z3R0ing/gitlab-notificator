package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MergeRequest {
    private Long id;

    private Long iid;

    private String title;

    private String state;

    private String action;

    private String url;

    @JsonProperty("target_branch")
    private String targetBranch;

    @JsonProperty("assignee_id")
    private Long assigneeId;

    private User assignee;

    private List<User> reviewers;

    @JsonProperty("draft")
    private Boolean isDraft;
}
