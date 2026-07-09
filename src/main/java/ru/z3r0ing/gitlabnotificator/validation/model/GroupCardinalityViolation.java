package ru.z3r0ing.gitlabnotificator.validation.model;

import org.springframework.lang.Nullable;

import java.util.List;

/**
 * Active label count in a group is outside the configured min/max bounds.
 */
public record GroupCardinalityViolation(
        String groupName,
        List<String> matchedLabels,
        int min,
        @Nullable Integer max,
        int actual) implements Violation {
}
