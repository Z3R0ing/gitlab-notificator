package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.NoteEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Note;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NoteEventHandlerTest {

    private NoteEventHandler handler;

    @Mock
    private MessageFormatter messageFormatter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new NoteEventHandler(messageFormatter);
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueForNoteEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.NOTE)).isTrue();
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnFalseForOtherEvents() {
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void handleNonMergeRequestNotes() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();
        event.getNote().setNoteableType("issue"); // Not a merge request
        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void handleEvent_ShouldReturnEmptyListWhenMergeRequestIsNull() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();
        event.setMergeRequest(null); // No merge request data
        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void handleEvent_ShouldNotifyAssigneeAndReviewers() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();

        // Set up assignee
        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);

        // Set up reviewers
        User reviewer1 = new User();
        reviewer1.setId(3L);
        reviewer1.setName("Reviewer 1");
        User reviewer2 = new User();
        reviewer2.setId(4L);
        reviewer2.setName("Reviewer 2");
        event.getMergeRequest().setReviewers(List.of(reviewer1, reviewer2));

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(3); // assignee + 2 reviewers
        assertThat(result).extracting(HandledEvent::getGitlabUserReceiverId).containsExactly(2L, 3L, 4L);

        verify(messageFormatter).formatNewCommentForMr("Test Project", "Test MR", "Comment Author");
        verify(messageFormatter).buttonsForNote("http://gitlab/test");
    }

    @Test
    void handleEvent_ShouldSkipAssigneeWhenSameAsAuthor() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();

        // Set up assignee same as author
        User assignee = new User();
        assignee.setId(1L); // Same as author
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);

        // Set up reviewers
        User reviewer = new User();
        reviewer.setId(3L);
        reviewer.setName("Reviewer");
        event.getMergeRequest().setReviewers(List.of(reviewer));

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(1); // Only reviewer should be notified
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(3L);
    }

    @Test
    void handleEvent_ShouldSkipReviewerWhenSameAsAuthor() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();

        // Set up assignee
        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);

        // Set up reviewer same as author
        User reviewer = new User();
        reviewer.setId(1L); // Same as author
        reviewer.setName("Reviewer");
        event.getMergeRequest().setReviewers(List.of(reviewer));

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(1); // Only assignee should be notified
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(2L);
    }

    @Test
    void handleEvent_ShouldHandleNoAssignee() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();
        event.getMergeRequest().setAssignee(null); // No assignee

        // Set up reviewers
        User reviewer = new User();
        reviewer.setId(3L);
        reviewer.setName("Reviewer");
        event.getMergeRequest().setReviewers(List.of(reviewer));

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(1); // Only reviewer should be notified
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(3L);
    }

    @Test
    void handleEvent_ShouldHandleNoReviewers() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();
        event.getMergeRequest().setReviewers(null); // No reviewers

        // Set up assignee
        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(1); // Only assignee should be notified
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(2L);
    }

    @Test
    void handleEvent_ShouldHandleEmptyReviewersList() throws JsonProcessingException {
        // Given
        NoteEvent event = createBasicNoteEvent();
        event.getMergeRequest().setReviewers(Collections.emptyList()); // Empty reviewers list

        // Set up assignee
        User assignee = new User();
        assignee.setId(2L);
        assignee.setName("Assignee");
        event.getMergeRequest().setAssignee(assignee);

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New comment on MR";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewCommentForMr(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForNote("http://gitlab/test")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(1); // Only assignee should be notified
        assertThat(result.get(0).getGitlabUserReceiverId()).isEqualTo(2L);
    }

    @Test
    void handleEvent_ShouldHandleInvalidJson() {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        assertThatThrownBy(() -> handler.handleEvent(invalidPayload))
                .isInstanceOf(JsonProcessingException.class);
    }

    private NoteEvent createBasicNoteEvent() {
        NoteEvent event = new NoteEvent();

        Project project = new Project();
        project.setName("Test Project");
        event.setProject(project);

        MergeRequest mergeRequest = new MergeRequest();
        mergeRequest.setTitle("Test MR");
        mergeRequest.setUrl("http://gitlab/test");
        event.setMergeRequest(mergeRequest);

        User author = new User();
        author.setId(1L);
        author.setName("Comment Author");
        event.setUser(author);

        Note note = new Note();
        note.setNoteableType("mergerequest");
        event.setNote(note);

        return event;
    }

    private List<InlineKeyboardButtonRow> createMockKeyboard() {
        InlineKeyboardButtonRow.InlineKeyboardButton button =
                InlineKeyboardButtonRow.InlineKeyboardButton.builder()
                        .text("View Comment")
                        .url("http://gitlab/test")
                        .build();

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(List.of(button));
        return List.of(row);
    }
}