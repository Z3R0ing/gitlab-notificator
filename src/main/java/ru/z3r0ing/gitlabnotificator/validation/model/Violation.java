package ru.z3r0ing.gitlabnotificator.validation.model;

/**
 * A single label rule violation. Implementations carry structured data.
 * Human-readable text is produced by MessageFormatter only.
 */
public sealed interface Violation permits GroupCardinalityViolation, UnknownLabelViolation {
}
