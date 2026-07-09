package ru.z3r0ing.gitlabnotificator.validation.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

/**
 * Runs every LabelRule bean against a snapshot and aggregates the violations.
 * Spring injects all LabelRule implementations - adding a rule type
 * requires no changes here.
 */
@Component
@RequiredArgsConstructor
public class LabelValidationEngine {

    private final List<LabelRule> rules;

    /**
     * Validates the snapshot against all configured rules.
     *
     * @param snapshot issue state from the last webhook event
     * @return violations from all rules; empty if the snapshot is fully valid
     */
    public List<Violation> validate(IssueSnapshot snapshot) {
        return rules.stream()
                .flatMap(rule -> rule.validate(snapshot).stream())
                .toList();
    }
}
