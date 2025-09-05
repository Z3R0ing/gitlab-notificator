package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.MergeRequestEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MergeRequestEventHandlerTest {

    private MergeRequestEventHandler handler;

    @Mock
    private MessageFormatter messageFormatter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new MergeRequestEventHandler(messageFormatter);
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueForMergeRequestEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isTrue();
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnFalseForOtherEvents() {
        assertThat(handler.doesSupportSuchEvent(EventType.ISSUE)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void formatMessageForEvent_ShouldReturnEmptyListForClosedMergeRequest() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();
        event.getMergeRequest().setState("closed");
        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void formatMessageForEvent_ShouldHandleOpenMergeRequest() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();
        event.getMergeRequest().setAction("open");

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New MR created";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        verify(messageFormatter).formatNewMr("Test Project", "Test MR", "Test User");
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldHandleDraftRemoval() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        MergeRequestEvent.DraftChanges draftChange = new MergeRequestEvent.DraftChanges();
        draftChange.setCurrent(false);
        changes.setDraft(draftChange);
        event.setChanges(changes);

        event.getMergeRequest().setAction("update");

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "Draft removed";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatMrUndraft(anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        verify(messageFormatter).formatMrUndraft("Test Project", "Test MR");
        // Исправлено: buttonsForMr вызывается только 1 раз
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldHandleReviewerAssignment() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        MergeRequestEvent.ReviewersChanges reviewersChanges = new MergeRequestEvent.ReviewersChanges();
        User reviewer1 = new User();
        reviewer1.setId(1L);
        reviewer1.setName("Reviewer 1");
        User reviewer2 = new User();
        reviewer2.setId(2L);
        reviewer2.setName("Reviewer 2");
        reviewersChanges.setCurrent(List.of(reviewer1, reviewer2));
        changes.setReviewers(reviewersChanges);

        event.getMergeRequest().setAction("update");

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "You are reviewer";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatYouAreMrReviewerNow(anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HandledEvent::getGitlabUserReceiverId).containsExactly(1L, 2L);
        verify(messageFormatter, times(2)).formatYouAreMrReviewerNow("Test Project", "Test MR");
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldSkipReviewerAssignmentWhenReviewerIsActionUser() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        User reviewer = new User();
        reviewer.setId(999L); // Same as action user
        reviewer.setName("Action User");
        event.setReviewers(List.of(reviewer));

        event.getMergeRequest().setAction("update");

        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void formatMessageForEvent_ShouldHandleApprovalAction() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();
        event.getMergeRequest().setAction("approved");

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);
        event.getMergeRequest().setAssigneeId(assignee.getId());

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "MR approved";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatMrApproved(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(2); // One for assignee, one impersonal
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(2L); // Assignee notification
        assertThat(result.get(1).getGitlabUserReceiverId()).isNull(); // Impersonal notification
        assertThat(result.get(1).getUserRole()).isEqualTo(UserRole.LEAD); // Impersonal notification to LEAD
        verify(messageFormatter).formatMrApproved("Test Project", "Test MR", "Test User");
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldHandleApprovalActionWithoutAssignee() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();
        event.getMergeRequest().setAction("approved");
        event.getMergeRequest().setAssignee(null);

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "MR approved";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatMrApproved(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(0).getUserRole()).isEqualTo(UserRole.LEAD); // Impersonal notification to LEAD
        verify(messageFormatter).formatMrApproved("Test Project", "Test MR", "Test User");
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldHandleMergeAction() throws JsonProcessingException {
        // Given
        MergeRequestEvent event = createBasicMergeRequestEvent();
        //event.getMergeRequest().setState("merged");
        event.getMergeRequest().setAction("merge");

        MergeRequestEvent.Changes changes = new MergeRequestEvent.Changes();
        event.setChanges(changes);

        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);
        event.getMergeRequest().setAssigneeId(assignee.getId());

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "MR merged";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatMrMerged(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForMr("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(3); // One for assignee, one impersonal
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(2L); // Assignee notification
        assertThat(result.get(1).getGitlabUserReceiverId()).isNull(); // Impersonal notification
        assertThat(result.get(1).getUserRole()).isEqualTo(UserRole.LEAD); // Impersonal notification to LEAD
        assertThat(result.get(2).getGitlabUserReceiverId()).isNull(); // Impersonal notification
        assertThat(result.get(2).getUserRole()).isEqualTo(UserRole.PM); // Impersonal notification to PM
        verify(messageFormatter).formatMrMerged("Test Project", "Test MR", "Test User");
        verify(messageFormatter, times(1)).buttonsForMr("http://gitlab/test");
    }

    @Test
    void formatMessageForEvent_ShouldHandleInvalidJson() {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        assertThatThrownBy(() -> handler.formatMessageForEvent(invalidPayload))
                .isInstanceOf(JsonProcessingException.class);
    }

    private MergeRequestEvent createBasicMergeRequestEvent() {
        MergeRequestEvent event = new MergeRequestEvent();

        Project project = new Project();
        project.setName("Test Project");
        event.setProject(project);

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setTitle("Test MR");
        mergeRequest.setState("opened");
        mergeRequest.setAction("open");
        mergeRequest.setUrl("http://gitlab/test");
        event.setMergeRequest(mergeRequest);

        User user = new User();
        user.setId(999L);
        user.setName("Test User");
        event.setUser(user);

        return event;
    }

    private List<InlineKeyboardButtonRow> createMockKeyboard() {
        InlineKeyboardButtonRow.InlineKeyboardButton button =
                InlineKeyboardButtonRow.InlineKeyboardButton.builder()
                        .text("View MR")
                        .url("http://gitlab/test")
                        .build();

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(List.of(button));
        return List.of(row);
    }
}