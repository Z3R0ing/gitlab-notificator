package ru.z3r0ing.gitlabnotificator.validation.engine;

import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

/**
 * A single validation rule over an issue snapshot.
 * Implementations are Spring beans; the engine discovers them via DI,
 * so adding a rule type requires no registration.
 */
public interface LabelRule {

    /**
     * Validates the snapshot against this rule.
     *
     * @param snapshot issue state from the last webhook event
     * @return violations found; empty if the rule is satisfied
     */
    List<Violation> validate(IssueSnapshot snapshot);

}
