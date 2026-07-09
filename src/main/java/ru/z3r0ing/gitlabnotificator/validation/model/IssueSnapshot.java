package ru.z3r0ing.gitlabnotificator.validation.model;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Immutable view of an issue at the moment of a webhook event.
 * Carries the full current label set - GitLab issue hooks always send it.
 */
public record IssueSnapshot(
        Long projectId,
        Long issueIid,
        String projectName,
        String issueTitle,
        String issueUrl,
        List<String> labels,
        @Nullable Long actorGitlabId,
        @Nullable String actorName,
        String action) {
}
