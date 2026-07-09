package ru.z3r0ing.gitlabnotificator.validation.scheduling;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;
import ru.z3r0ing.gitlabnotificator.service.NotificationService;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;
import ru.z3r0ing.gitlabnotificator.validation.config.LabelRulesProperties;
import ru.z3r0ing.gitlabnotificator.validation.engine.LabelValidationEngine;
import ru.z3r0ing.gitlabnotificator.validation.model.GroupCardinalityViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueValidationSchedulerTest {

    private static final IssueKey KEY = new IssueKey(1L, 42L);
    private static final Violation VIOLATION =
            new GroupCardinalityViolation("Status", List.of(), 1, 1, 0);
    private static final Violation OTHER_VIOLATION =
            new GroupCardinalityViolation("Type", List.of(), 1, 1, 0);

    @Mock
    private LabelValidationEngine engine;
    @Mock
    private NotificationService notificationService;
    @Mock
    private MessageFormatter messageFormatter;
    @Mock
    private UserMappingRepository userMappingRepository;

    private final LabelRulesProperties properties = new LabelRulesProperties();
    private final IssueValidationStateStore stateStore = new IssueValidationStateStore();
    private final ManualTaskScheduler taskScheduler = new ManualTaskScheduler();

    private IssueValidationScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new IssueValidationScheduler(engine, notificationService, messageFormatter,
                properties, userMappingRepository, stateStore, taskScheduler);
        lenient().when(messageFormatter.formatLabelViolations(anyString(), anyString(), anyList()))
                .thenReturn("violations");
        lenient().when(messageFormatter.formatLabelViolationsEscalated(
                        anyString(), anyString(), anyList(), anyString(), anyString()))
                .thenReturn("escalated");
        lenient().when(messageFormatter.buttonsForIssue(anyString()))
                .thenReturn(Collections.emptyList());
    }

    private IssueSnapshot snapshot(String action, List<String> labels) {
        return new IssueSnapshot(KEY.projectId(), KEY.issueIid(), "smartdebt", "Test issue",
                "http://gitlab/issue/42", labels, 146L, "Roman Petrov", action);
    }

    private void mapActor() {
        when(userMappingRepository.findByGitlabUserId(146L))
                .thenReturn(Optional.of(new UserMapping(1L, 500L, 146L, UserRole.DEV)));
    }

    @Test
    void onIssueEvent_ShouldScheduleSingleDebounceTask() {
        scheduler.onIssueEvent(snapshot("open", List.of()));

        assertThat(taskScheduler.activeTasks()).hasSize(1);
        assertThat(stateStore.find(KEY)).isPresent();
    }

    @Test
    void onIssueEvent_SecondEventBeforeDebounce_ShouldRearmTimerAndKeepLatestSnapshot() {
        scheduler.onIssueEvent(snapshot("open", List.of()));
        scheduler.onIssueEvent(snapshot("update", List.of("S:Review")));

        assertThat(taskScheduler.tasks.get(0).isCancelled()).isTrue();
        assertThat(taskScheduler.activeTasks()).hasSize(1);

        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of());
        taskScheduler.lastTask().run();

        ArgumentCaptor<IssueSnapshot> captor = ArgumentCaptor.forClass(IssueSnapshot.class);
        verify(engine).validate(captor.capture());
        assertThat(captor.getValue().labels()).containsExactly("S:Review");
    }

    @Test
    void debounce_NoViolations_ShouldClearStateSilently() {
        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of());
        scheduler.onIssueEvent(snapshot("open", List.of()));

        taskScheduler.lastTask().run();

        assertThat(stateStore.find(KEY)).isEmpty();
        verify(notificationService, never()).send(any(HandledEvent.class));
    }

    @Test
    void debounce_Violations_ShouldNotifyActorAndArmEscalation() {
        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of(VIOLATION));
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));

        taskScheduler.lastTask().run();

        ArgumentCaptor<HandledEvent> captor = ArgumentCaptor.forClass(HandledEvent.class);
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().getGitlabUserReceiverId()).isEqualTo(146L);
        assertThat(taskScheduler.activeTasks()).hasSize(1); // the escalation task
        assertThat(stateStore.find(KEY)).isPresent();
    }

    @Test
    void debounce_ActorNotMapped_ShouldNotifyLeadInstead() {
        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of(VIOLATION));
        when(userMappingRepository.findByGitlabUserId(146L)).thenReturn(Optional.empty());
        scheduler.onIssueEvent(snapshot("open", List.of()));

        taskScheduler.lastTask().run();

        ArgumentCaptor<HandledEvent> captor = ArgumentCaptor.forClass(HandledEvent.class);
        verify(notificationService, times(1)).send(captor.capture());
        assertThat(captor.getValue().getUserRole()).isEqualTo(UserRole.LEAD);
    }

    @Test
    void onIssueEvent_CloseAction_ShouldCancelTimersAndClearState() {
        scheduler.onIssueEvent(snapshot("open", List.of()));

        scheduler.onIssueEvent(snapshot("close", List.of()));

        assertThat(stateStore.find(KEY)).isEmpty();
        assertThat(taskScheduler.activeTasks()).isEmpty();
    }

    @Test
    void debounce_SameViolationsWhileEscalationArmed_ShouldNotNotifyAgain() {
        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of(VIOLATION));
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));
        taskScheduler.lastTask().run(); // first notification, escalation armed

        scheduler.onIssueEvent(snapshot("update", List.of("S:Review")));
        taskScheduler.lastTask().run(); // debounce again, same violations

        verify(notificationService, times(1)).send(any(HandledEvent.class));
    }

    @Test
    void debounce_ChangedViolationsWhileEscalationArmed_ShouldNotifyAgainWithoutNewEscalation() {
        when(engine.validate(any(IssueSnapshot.class)))
                .thenReturn(List.of(VIOLATION))
                .thenReturn(List.of(OTHER_VIOLATION));
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));
        taskScheduler.lastTask().run(); // notification 1 + escalation armed
        int tasksAfterFirstRun = taskScheduler.tasks.size();
        java.util.concurrent.ScheduledFuture<?> escalationBefore =
                stateStore.find(KEY).orElseThrow().getEscalationTask();

        scheduler.onIssueEvent(snapshot("update", List.of("T:Bug")));
        taskScheduler.lastTask().run(); // notification 2, escalation unchanged

        verify(notificationService, times(2)).send(any(HandledEvent.class));
        // one new task only (the re-armed debounce), no second escalation task
        assertThat(taskScheduler.tasks).hasSize(tasksAfterFirstRun + 1);
        assertThat((Object) stateStore.find(KEY).orElseThrow().getEscalationTask()).isSameAs(escalationBefore);
        assertThat(escalationBefore.isCancelled()).isFalse();
    }

    @Test
    void debounce_ViolationsFixedWhileEscalationArmed_ShouldCancelEscalationAndClearState() {
        when(engine.validate(any(IssueSnapshot.class)))
                .thenReturn(List.of(VIOLATION))
                .thenReturn(List.of());
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));
        taskScheduler.lastTask().run(); // notification + escalation armed

        scheduler.onIssueEvent(snapshot("update", List.of("S:Review")));
        taskScheduler.lastTask().run(); // clean validation

        assertThat(stateStore.find(KEY)).isEmpty();
        assertThat(taskScheduler.activeTasks()).isEmpty();
    }

    @Test
    void escalation_ViolationsStillPresent_ShouldNotifyLeadAndEndCycle() {
        properties.setEscalationDelay(java.time.Duration.ofHours(1));
        when(engine.validate(any(IssueSnapshot.class))).thenReturn(List.of(VIOLATION));
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));
        taskScheduler.lastTask().run(); // notify Roman, escalation armed, firstNotified = Roman Petrov

        // a different actor touches the issue before the deadline
        scheduler.onIssueEvent(new IssueSnapshot(KEY.projectId(), KEY.issueIid(), "smartdebt",
                "Test issue", "http://gitlab/issue/42", List.of("T:Bug"), 999L, "Ivan Ivanov", "update"));
        taskScheduler.lastTask().run(); // same violation set -> no re-notify

        taskScheduler.activeTasks().get(0).run(); // fire the original escalation task

        ArgumentCaptor<HandledEvent> captor = ArgumentCaptor.forClass(HandledEvent.class);
        verify(notificationService, times(2)).send(captor.capture());
        assertThat(captor.getAllValues().get(1).getUserRole()).isEqualTo(UserRole.LEAD);
        // must cite the first notified user, not the latest actor (Ivan Ivanov)
        verify(messageFormatter).formatLabelViolationsEscalated(
                anyString(), anyString(), anyList(), eq("Roman Petrov"), eq("1h"));
        assertThat(stateStore.find(KEY)).isEmpty();
    }

    @Test
    void escalation_ViolationsFixedJustBeforeDeadline_ShouldNotNotifyLead() {
        when(engine.validate(any(IssueSnapshot.class)))
                .thenReturn(List.of(VIOLATION))
                .thenReturn(List.of());
        mapActor();
        scheduler.onIssueEvent(snapshot("open", List.of()));
        taskScheduler.lastTask().run(); // notification + escalation armed

        taskScheduler.lastTask().run(); // escalation fires, but violations are gone

        verify(notificationService, times(1)).send(any(HandledEvent.class));
        assertThat(stateStore.find(KEY)).isEmpty();
    }
}
