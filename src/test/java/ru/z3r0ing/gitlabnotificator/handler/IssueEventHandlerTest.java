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
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.IssueEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueEventHandlerTest {

    private IssueEventHandler handler;

    @Mock
    private MessageFormatter messageFormatter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new IssueEventHandler(messageFormatter);
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueForIssueEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.ISSUE)).isTrue();
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnFalseForOtherEvents() {
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void formatMessageForEvent_ShouldReturnEmptyListForClosedIssue() throws JsonProcessingException {
        // Given
        IssueEvent event = createBasicIssueEvent();
        event.getIssue().setState("closed");
        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void formatMessageForEvent_ShouldHandleOpenIssue() throws JsonProcessingException {
        // Given
        IssueEvent event = createBasicIssueEvent();
        event.getIssue().setState("opened");
        event.getIssue().setAction("open");
        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New issue created";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewIssue(anyString(), anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForIssue("http://gitlab/issue/1")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        verify(messageFormatter).formatNewIssue("Test Project", "Test Issue", "Test User");
        verify(messageFormatter).buttonsForIssue("http://gitlab/issue/1");
    }

    @Test
    void formatMessageForEvent_ShouldReturnEmptyListForNonOpenAction() throws JsonProcessingException {
        // Given
        IssueEvent event = createBasicIssueEvent();
        event.getIssue().setState("opened");
        event.getIssue().setAction("update"); // Not "open" action
        String payload = objectMapper.writeValueAsString(event);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
        verify(messageFormatter, never()).formatNewIssue(anyString(), anyString(), anyString());
        verify(messageFormatter, never()).buttonsForIssue(anyString());
    }

    @Test
    void formatMessageForEvent_ShouldHandleInvalidJson() {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        assertThatThrownBy(() -> handler.formatMessageForEvent(invalidPayload))
                .isInstanceOf(JsonProcessingException.class);
    }

    private IssueEvent createBasicIssueEvent() {
        IssueEvent event = new IssueEvent();

        Project project = new Project();
        project.setName("Test Project");
        event.setProject(project);

        ru.z3r0ing.gitlabnotificator.model.gitlab.object.Issue issue =
                new ru.z3r0ing.gitlabnotificator.model.gitlab.object.Issue();
        issue.setTitle("Test Issue");
        issue.setState("opened");
        issue.setAction("open");
        issue.setUrl("http://gitlab/issue/1");
        event.setIssue(issue);

        User user = new User();
        user.setId(999L);
        user.setName("Test User");
        event.setUser(user);

        return event;
    }

    private List<InlineKeyboardButtonRow> createMockKeyboard() {
        InlineKeyboardButtonRow.InlineKeyboardButton button =
                InlineKeyboardButtonRow.InlineKeyboardButton.builder()
                        .text("View Issue")
                        .url("http://gitlab/issue/1")
                        .build();

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(List.of(button));
        return List.of(row);
    }
}