package ru.z3r0ing.gitlabnotificator.validation.engine;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.UnknownLabelViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.Collections;
import java.util.List;

/**
 * Flags labels that do not belong to any configured group.
 * Disabled entirely when label-rules.forbid-unknown-labels is false.
 */
@Component
@RequiredArgsConstructor
public class UnknownLabelRule implements LabelRule {

    private final LabelRulesProperties properties;

    @Override
    public List<Violation> validate(IssueSnapshot snapshot) {
        if (!properties.isForbidUnknownLabels()) {
            return Collections.emptyList();
        }
        List<String> unknownLabels = snapshot.labels().stream()
                .filter(label -> properties.getGroups().stream()
                        .noneMatch(group -> label.startsWith(group.getPrefix())))
                .toList();
        if (unknownLabels.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new UnknownLabelViolation(unknownLabels));
    }
}
