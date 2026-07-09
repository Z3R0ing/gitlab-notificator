package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;
import ru.z3r0ing.gitlabnotificator.service.NotificationService;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.engine.LabelValidationEngine;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Orchestrates the per-issue validation cycle: debounces incoming events,
 * validates the settled snapshot, notifies the change author and escalates
 * to LEAD when violations are not fixed in time.
 */
@Component
@Slf4j
public class IssueValidationScheduler {

    private final LabelValidationEngine engine;
    private final NotificationService notificationService;
    private final MessageFormatter messageFormatter;
    private final LabelRulesProperties properties;
    private final UserMappingRepository userMappingRepository;
    private final IssueValidationStateStore stateStore;
    private final TaskScheduler taskScheduler;

    public IssueValidationScheduler(LabelValidationEngine engine,
                                    NotificationService notificationService,
                                    MessageFormatter messageFormatter,
                                    LabelRulesProperties properties,
                                    UserMappingRepository userMappingRepository,
                                    IssueValidationStateStore stateStore,
                                    @Qualifier("labelValidationTaskScheduler") TaskScheduler taskScheduler) {
        this.engine = engine;
        this.notificationService = notificationService;
        this.messageFormatter = messageFormatter;
        this.properties = properties;
        this.userMappingRepository = userMappingRepository;
        this.stateStore = stateStore;
        this.taskScheduler = taskScheduler;
    }

    /**
     * Accepts an issue event: closing clears the cycle, anything else
     * replaces the snapshot and re-arms the debounce timer.
     *
     * @param snapshot issue state from the webhook payload
     */
    public void onIssueEvent(IssueSnapshot snapshot) {
        IssueKey key = new IssueKey(snapshot.projectId(), snapshot.issueIid());
        if ("close".equalsIgnoreCase(snapshot.action())) {
            clearCycle(key);
            return;
        }
        stateStore.mutate(key, existing -> {
            IssueValidationState state = existing != null ? existing : new IssueValidationState();
            state.setSnapshot(snapshot);
            cancelIfPresent(state.getDebounceTask());
            state.setDebounceTask(taskScheduler.schedule(() -> runDebouncedValidation(key),
                    Instant.now().plus(properties.getDebounce())));
            return state;
        });
    }

    private void runDebouncedValidation(IssueKey key) {
        try {
            AtomicReference<Runnable> sideEffect = new AtomicReference<>();
            stateStore.mutate(key, state -> {
                if (state == null) {
                    return null; // orphan callback - the cycle was cleared meanwhile
                }
                state.setDebounceTask(null);
                List<Violation> violations = engine.validate(state.getSnapshot());
                if (violations.isEmpty()) {
                    cancelIfPresent(state.getEscalationTask());
                    return null; // cycle closes silently
                }
                Set<Violation> violationSet = new HashSet<>(violations);
                IssueSnapshot snapshot = state.getSnapshot();
                if (state.getEscalationTask() == null) {
                    state.setEscalationTask(taskScheduler.schedule(() -> runEscalation(key),
                            Instant.now().plus(properties.getEscalationDelay())));
                    state.setFirstNotifiedUserName(
                            snapshot.actorName() != null ? snapshot.actorName() : "unknown");
                    state.setLastNotifiedViolations(violationSet);
                    sideEffect.set(() -> notifyAuthor(snapshot, violations));
                } else if (!violationSet.equals(state.getLastNotifiedViolations())) {
                    // escalation deadline intentionally not moved
                    state.setLastNotifiedViolations(violationSet);
                    sideEffect.set(() -> notifyAuthor(snapshot, violations));
                }
                return state;
            });
            runIfPresent(sideEffect.get());
        } catch (RuntimeException e) {
            log.error("Label validation failed for issue {}", key, e);
        }
    }

    private void runEscalation(IssueKey key) {
        try {
            AtomicReference<Runnable> sideEffect = new AtomicReference<>();
            stateStore.mutate(key, state -> {
                if (state == null) {
                    return null;
                }
                cancelIfPresent(state.getDebounceTask());
                List<Violation> violations = engine.validate(state.getSnapshot());
                if (!violations.isEmpty()) {
                    IssueSnapshot snapshot = state.getSnapshot();
                    String notifiedUserName = state.getFirstNotifiedUserName() != null
                            ? state.getFirstNotifiedUserName() : "unknown";
                    sideEffect.set(() -> notifyLead(snapshot, violations, notifiedUserName));
                }
                return null; // escalation is one-shot: the cycle ends here either way
            });
            runIfPresent(sideEffect.get());
        } catch (RuntimeException e) {
            log.error("Label validation escalation failed for issue {}", key, e);
        }
    }

    private void notifyAuthor(IssueSnapshot snapshot, List<Violation> violations) {
        String message = messageFormatter.formatLabelViolations(
                snapshot.projectName(), snapshot.issueTitle(), violations);
        MessageWithKeyboard messageWithKeyboard = new MessageWithKeyboard(
                message, messageFormatter.buttonsForIssue(snapshot.issueUrl()));
        Long actorId = snapshot.actorGitlabId();
        if (actorId == null || userMappingRepository.findByGitlabUserId(actorId).isEmpty()) {
            log.warn("No user mapping for label violation author {}, notifying LEAD instead", actorId);
            notificationService.send(new HandledEvent(UserRole.LEAD, messageWithKeyboard));
            return;
        }
        notificationService.send(new HandledEvent(actorId, messageWithKeyboard));
    }

    private void notifyLead(IssueSnapshot snapshot, List<Violation> violations, String notifiedUserName) {
        String message = messageFormatter.formatLabelViolationsEscalated(
                snapshot.projectName(), snapshot.issueTitle(), violations,
                notifiedUserName, formatDuration(properties.getEscalationDelay()));
        notificationService.send(new HandledEvent(UserRole.LEAD, new MessageWithKeyboard(
                message, messageFormatter.buttonsForIssue(snapshot.issueUrl()))));
    }

    private void clearCycle(IssueKey key) {
        stateStore.mutate(key, state -> {
            if (state != null) {
                cancelIfPresent(state.getDebounceTask());
                cancelIfPresent(state.getEscalationTask());
            }
            return null;
        });
    }

    private void cancelIfPresent(@Nullable ScheduledFuture<?> task) {
        if (task != null) {
            task.cancel(false);
        }
    }

    private void runIfPresent(@Nullable Runnable sideEffect) {
        if (sideEffect != null) {
            sideEffect.run();
        }
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        if (hours > 0 && minutes == 0) {
            return hours + "h";
        }
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
