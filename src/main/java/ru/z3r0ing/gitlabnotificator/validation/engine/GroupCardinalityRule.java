package ru.z3r0ing.gitlabnotificator.validation.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.model.GroupCardinalityViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that the number of active labels in every configured group
 * stays within the group's min/max bounds.
 */
@Component
@RequiredArgsConstructor
public class GroupCardinalityRule implements LabelRule {

    private final LabelRulesProperties properties;

    @Override
    public List<Violation> validate(IssueSnapshot snapshot) {
        List<Violation> violations = new ArrayList<>();
        for (LabelRulesProperties.Group group : properties.getGroups()) {
            List<String> matchedLabels = snapshot.labels().stream()
                    .filter(label -> label.startsWith(group.getPrefix()))
                    .toList();
            int actual = matchedLabels.size();
            boolean belowMin = actual < group.getMin();
            boolean aboveMax = group.getMax() != null && actual > group.getMax();
            if (belowMin || aboveMax) {
                violations.add(new GroupCardinalityViolation(
                        group.getName(), matchedLabels, group.getMin(), group.getMax(), actual));
            }
        }
        return violations;
    }
}
