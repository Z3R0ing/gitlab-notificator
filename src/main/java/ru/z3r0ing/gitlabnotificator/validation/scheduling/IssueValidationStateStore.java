package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * Owns all per-issue validation state. The backing map is an implementation detail.
 * Callers express state transitions through atomic mutations.
 * Swapping this for a persistent implementation is the designated seam
 * for surviving restarts.
 */
@Component
public class IssueValidationStateStore {

    private final Map<IssueKey, IssueValidationState> states = new ConcurrentHashMap<>();

    /**
     * Applies an atomic state transition for the given issue.
     * The mutation receives the current state (or null if absent) and returns
     * the new state. Returning null removes the entry.
     * Calls for the same key never run concurrently.
     *
     * @param key      issue identity
     * @param mutation state transition to apply
     * @return the state after mutation, or null if the entry was removed
     */
    @Nullable
    public IssueValidationState mutate(IssueKey key, UnaryOperator<IssueValidationState> mutation) {
        return states.compute(key, (ignored, existing) -> mutation.apply(existing));
    }

    public Optional<IssueValidationState> find(IssueKey key) {
        return Optional.ofNullable(states.get(key));
    }

    public void remove(IssueKey key) {
        states.remove(key);
    }
}
