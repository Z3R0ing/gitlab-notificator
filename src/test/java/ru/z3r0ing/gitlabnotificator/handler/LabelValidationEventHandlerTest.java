package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.IssueEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Issue;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Label;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.scheduling.IssueValidationScheduler;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LabelValidationEventHandlerTest {

    private LabelValidationEventHandler handler;

    @Mock
    private IssueValidationScheduler scheduler;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new LabelValidationEventHandler(scheduler);
    }

    private IssueEvent createIssueEvent() {
        IssueEvent event = new IssueEvent();

        Issue issue = new Issue();
        issue.setIid(42L);
        issue.setTitle("Test issue");
        issue.setUrl("http://gitlab/issue/42");
        issue.setAction("update");
        issue.setState("opened");
        event.setIssue(issue);

        Project project = new Project();
        project.setId(1L);
        project.setName("smartdebt");
        event.setProject(project);

        User user = new User();
        user.setId(146L);
        user.setName("Roman Petrov");
        event.setUser(user);

        Label label = new Label();
        label.setTitle("S:Review");
        event.setLabels(List.of(label));

        return event;
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueOnlyForIssueEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.ISSUE)).isTrue();
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void handleEvent_ValidPayload_ShouldPassSnapshotToSchedulerAndReturnNoNotifications()
            throws JsonProcessingException {
        String payload = objectMapper.writeValueAsString(createIssueEvent());

        List<HandledEvent> result = handler.handleEvent(payload);

        assertThat(result).isEmpty();
        ArgumentCaptor<IssueSnapshot> captor = ArgumentCaptor.forClass(IssueSnapshot.class);
        verify(scheduler).onIssueEvent(captor.capture());
        IssueSnapshot snapshot = captor.getValue();
        assertThat(snapshot.projectId()).isEqualTo(1L);
        assertThat(snapshot.issueIid()).isEqualTo(42L);
        assertThat(snapshot.labels()).containsExactly("S:Review");
        assertThat(snapshot.actorGitlabId()).isEqualTo(146L);
        assertThat(snapshot.action()).isEqualTo("update");
    }

    @Test
    void handleEvent_NoLabels_ShouldPassEmptyLabelList() throws JsonProcessingException {
        IssueEvent event = createIssueEvent();
        event.setLabels(null);
        String payload = objectMapper.writeValueAsString(event);

        handler.handleEvent(payload);

        ArgumentCaptor<IssueSnapshot> captor = ArgumentCaptor.forClass(IssueSnapshot.class);
        verify(scheduler).onIssueEvent(captor.capture());
        assertThat(captor.getValue().labels()).isEmpty();
    }

    @Test
    void handleEvent_NoUser_ShouldPassNullActor() throws JsonProcessingException {
        IssueEvent event = createIssueEvent();
        event.setUser(null);
        String payload = objectMapper.writeValueAsString(event);

        handler.handleEvent(payload);

        ArgumentCaptor<IssueSnapshot> captor = ArgumentCaptor.forClass(IssueSnapshot.class);
        verify(scheduler).onIssueEvent(captor.capture());
        assertThat(captor.getValue().actorGitlabId()).isNull();
        assertThat(captor.getValue().actorName()).isNull();
    }

    @Test
    void handleEvent_MissingIssueOrProject_ShouldSkipScheduling() throws JsonProcessingException {
        IssueEvent event = createIssueEvent();
        event.setIssue(null);
        String payload = objectMapper.writeValueAsString(event);

        List<HandledEvent> result = handler.handleEvent(payload);

        assertThat(result).isEmpty();
        verify(scheduler, never()).onIssueEvent(any(IssueSnapshot.class));
    }

    @Test
    void handleEvent_ClosedIssueState_ShouldMapActionToClose() throws JsonProcessingException {
        IssueEvent event = createIssueEvent();
        event.getIssue().setState("closed");
        event.getIssue().setAction("update");
        String payload = objectMapper.writeValueAsString(event);

        handler.handleEvent(payload);

        ArgumentCaptor<IssueSnapshot> captor = ArgumentCaptor.forClass(IssueSnapshot.class);
        verify(scheduler).onIssueEvent(captor.capture());
        assertThat(captor.getValue().action()).isEqualTo("close");
    }
}
