package ru.z3r0ing.gitlabnotificator.validation.scheduling;

/**
 * Identity of an issue across webhook events.
 */
public record IssueKey(Long projectId, Long issueIid) {
}
