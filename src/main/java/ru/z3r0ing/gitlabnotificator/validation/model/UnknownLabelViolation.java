package ru.z3r0ing.gitlabnotificator.validation.model;

import java.util.List;

/**
 * Labels that do not match any configured group prefix.
 */
public record UnknownLabelViolation(List<String> unknownLabels) implements Violation {
}
