package ru.z3r0ing.gitlabnotificator.validation.engine;

import org.junit.jupiter.api.Test;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.model.GroupCardinalityViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GroupCardinalityRuleTest {

    private GroupCardinalityRule rule(LabelRulesProperties.Group... groups) {
        LabelRulesProperties properties = new LabelRulesProperties();
        properties.setGroups(List.of(groups));
        return new GroupCardinalityRule(properties);
    }

    private LabelRulesProperties.Group group(String name, String prefix, int min, Integer max) {
        LabelRulesProperties.Group group = new LabelRulesProperties.Group();
        group.setName(name);
        group.setPrefix(prefix);
        group.setMin(min);
        group.setMax(max);
        return group;
    }

    private IssueSnapshot snapshotWithLabels(List<String> labels) {
        return new IssueSnapshot(1L, 42L, "smartdebt", "Test issue", "http://gitlab/issue/42",
                labels, 146L, "Roman Petrov", "update");
    }

    @Test
    void validate_CountWithinBounds_ShouldReturnNoViolations() {
        GroupCardinalityRule cardinalityRule = rule(group("Status", "S:", 1, 1));
        IssueSnapshot snapshot = snapshotWithLabels(List.of("S:Review", "T:Bug"));

        assertThat(cardinalityRule.validate(snapshot)).isEmpty();
    }

    @Test
    void validate_TooManyLabelsInGroup_ShouldReturnViolationWithMatchedLabels() {
        GroupCardinalityRule cardinalityRule = rule(group("Status", "S:", 1, 1));
        IssueSnapshot snapshot = snapshotWithLabels(List.of("S:Review", "S:Testing", "T:Bug"));

        List<Violation> violations = cardinalityRule.validate(snapshot);

        assertThat(violations).containsExactly(new GroupCardinalityViolation(
                "Status", List.of("S:Review", "S:Testing"), 1, 1, 2));
    }

    @Test
    void validate_MissingMandatoryGroup_ShouldReturnViolation() {
        GroupCardinalityRule cardinalityRule = rule(group("Type", "T:", 1, 1));
        IssueSnapshot snapshot = snapshotWithLabels(List.of("S:Review"));

        List<Violation> violations = cardinalityRule.validate(snapshot);

        assertThat(violations).containsExactly(new GroupCardinalityViolation(
                "Type", List.of(), 1, 1, 0));
    }

    @Test
    void validate_NoMaxLimit_ShouldAllowManyLabels() {
        GroupCardinalityRule cardinalityRule = rule(group("Environment", "E:", 0, null));
        IssueSnapshot snapshot = snapshotWithLabels(List.of("E:Test", "E:Stage", "E:Иркутск"));

        assertThat(cardinalityRule.validate(snapshot)).isEmpty();
    }

    @Test
    void validate_MultipleGroupsBroken_ShouldReturnViolationPerGroup() {
        GroupCardinalityRule cardinalityRule = rule(
                group("Status", "S:", 1, 1),
                group("Type", "T:", 1, 1));
        IssueSnapshot snapshot = snapshotWithLabels(List.of("S:Review", "S:Testing"));

        assertThat(cardinalityRule.validate(snapshot)).hasSize(2);
    }

    @Test
    void validate_EmptyLabels_ShouldReportOnlyMandatoryGroups() {
        GroupCardinalityRule cardinalityRule = rule(
                group("Status", "S:", 1, 1),
                group("Blocked", "B:", 0, 1));
        IssueSnapshot snapshot = snapshotWithLabels(List.of());

        List<Violation> violations = cardinalityRule.validate(snapshot);

        assertThat(violations).containsExactly(new GroupCardinalityViolation(
                "Status", List.of(), 1, 1, 0));
    }
}
