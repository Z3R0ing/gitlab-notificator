package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import lombok.Data;
import org.springframework.lang.Nullable;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.Set;
import java.util.concurrent.ScheduledFuture;

/**
 * Per-issue validation cycle state. Mutated only inside
 * IssueValidationStateStore.mutate() - never shared outside a mutation.
 */
@Data
public class IssueValidationState {

    private IssueSnapshot snapshot;

    @Nullable
    private ScheduledFuture<?> debounceTask;

    @Nullable
    private ScheduledFuture<?> escalationTask;

    @Nullable
    private Set<Violation> lastNotifiedViolations;

    @Nullable
    private String firstNotifiedUserName;
}
