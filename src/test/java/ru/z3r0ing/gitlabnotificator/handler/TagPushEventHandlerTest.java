package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.TagPushEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagPushEventHandlerTest {

    private TagPushEventHandler handler;

    @Mock
    private MessageFormatter messageFormatter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new TagPushEventHandler(messageFormatter);
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueForTagPushEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.TAG_PUSH)).isTrue();
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnFalseForOtherEvents() {
        assertThat(handler.doesSupportSuchEvent(EventType.ISSUE)).isFalse();
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void handleEvent_ShouldHandleTagPushEvent() throws JsonProcessingException {
        // Given
        TagPushEvent event = createBasicTagPushEvent();
        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "New tag created";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatNewTag(anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForTag("http://gitlab/-/tags/v1.0.0")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.handleEvent(payload);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(0).getUserRole()).isEqualTo(UserRole.LEAD);
        assertThat(result.get(1).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(1).getUserRole()).isEqualTo(UserRole.PM);
        verify(messageFormatter).formatNewTag("Test Project", "v1.0.0");
        verify(messageFormatter).buttonsForTag("http://gitlab/-/tags/v1.0.0");
    }

    @Test
    void handleEvent_ShouldHandleInvalidJson() {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        assertThatThrownBy(() -> handler.handleEvent(invalidPayload))
                .isInstanceOf(JsonProcessingException.class);
    }

    private TagPushEvent createBasicTagPushEvent() {
        TagPushEvent event = new TagPushEvent();

        Project project = new Project();
        project.setName("Test Project");
        project.setWebUrl("http://gitlab"); // Добавьте URL проекта
        event.setProject(project);

        event.setTagReference("refs/tags/v1.0.0");

        return event;
    }

    private List<InlineKeyboardButtonRow> createMockKeyboard() {
        InlineKeyboardButtonRow.InlineKeyboardButton button =
                InlineKeyboardButtonRow.InlineKeyboardButton.builder()
                        .text("View Tag")
                        .url("http://gitlab/tag/v1.0.0")
                        .build();

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(List.of(button));
        return List.of(row);
    }
}