package ru.z3r0ing.gitlabnotificator.validation.engine;

import org.junit.jupiter.api.Test;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.UnknownLabelViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnknownLabelRuleTest {

    private UnknownLabelRule rule(boolean forbidUnknownLabels, String... prefixes) {
        LabelRulesProperties properties = new LabelRulesProperties();
        properties.setForbidUnknownLabels(forbidUnknownLabels);
        properties.setGroups(java.util.Arrays.stream(prefixes).map(prefix -> {
            LabelRulesProperties.Group group = new LabelRulesProperties.Group();
            group.setName(prefix);
            group.setPrefix(prefix);
            return group;
        }).toList());
        return new UnknownLabelRule(properties);
    }

    private IssueSnapshot snapshotWithLabels(List<String> labels) {
        return new IssueSnapshot(1L, 42L, "smartdebt", "Test issue", "http://gitlab/issue/42",
                labels, 146L, "Roman Petrov", "update");
    }

    @Test
    void validate_AllLabelsMatchSomePrefix_ShouldReturnNoViolations() {
        UnknownLabelRule unknownLabelRule = rule(true, "S:", "T:");

        assertThat(unknownLabelRule.validate(snapshotWithLabels(List.of("S:Review", "T:Bug")))).isEmpty();
    }

    @Test
    void validate_LabelsOutsideAllGroups_ShouldReturnSingleViolationWithAllOfThem() {
        UnknownLabelRule unknownLabelRule = rule(true, "S:", "T:");
        IssueSnapshot snapshot = snapshotWithLabels(List.of("S:Review", "В работе", "Выполнена"));

        List<Violation> violations = unknownLabelRule.validate(snapshot);

        assertThat(violations).containsExactly(
                new UnknownLabelViolation(List.of("В работе", "Выполнена")));
    }

    @Test
    void validate_ForbidDisabled_ShouldReturnNoViolations() {
        UnknownLabelRule unknownLabelRule = rule(false, "S:");

        assertThat(unknownLabelRule.validate(snapshotWithLabels(List.of("В работе")))).isEmpty();
    }
}
