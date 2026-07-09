package ru.z3r0ing.gitlabnotificator.validation.engine;

import org.junit.jupiter.api.Test;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.UnknownLabelViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LabelValidationEngineTest {

    private final IssueSnapshot snapshot = new IssueSnapshot(1L, 42L, "smartdebt", "Test issue",
            "http://gitlab/issue/42", List.of(), 146L, "Roman Petrov", "update");

    @Test
    void validate_ShouldAggregateViolationsFromAllRules() {
        LabelRule firstRule = mock(LabelRule.class);
        LabelRule secondRule = mock(LabelRule.class);
        Violation violation = new UnknownLabelViolation(List.of("В работе"));
        when(firstRule.validate(snapshot)).thenReturn(List.of());
        when(secondRule.validate(snapshot)).thenReturn(List.of(violation));

        LabelValidationEngine engine = new LabelValidationEngine(List.of(firstRule, secondRule));

        assertThat(engine.validate(snapshot)).containsExactly(violation);
    }

    @Test
    void validate_NoRules_ShouldReturnEmptyList() {
        LabelValidationEngine engine = new LabelValidationEngine(List.of());

        assertThat(engine.validate(snapshot)).isEmpty();
    }
}
