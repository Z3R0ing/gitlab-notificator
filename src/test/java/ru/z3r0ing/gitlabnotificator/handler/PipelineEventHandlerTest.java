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
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.PipelineEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Pipeline;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Project;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineEventHandlerTest {

    private PipelineEventHandler handler;

    @Mock
    private MessageFormatter messageFormatter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        handler = new PipelineEventHandler(messageFormatter);
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnTrueForPipelineEvent() {
        assertThat(handler.doesSupportSuchEvent(EventType.PIPELINE)).isTrue();
    }

    @Test
    void doesSupportSuchEvent_ShouldReturnFalseForOtherEvents() {
        assertThat(handler.doesSupportSuchEvent(EventType.MERGE_REQUEST)).isFalse();
        assertThat(handler.doesSupportSuchEvent(null)).isFalse();
    }

    @Test
    void formatMessageForEvent_ShouldHandleFailedPipelineWithoutMergeRequest() throws JsonProcessingException {
        // Given
        PipelineEvent event = createBasicPipelineEvent();
        event.getPipeline().setStatus("failed");
        event.setMergeRequest(null);

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "Pipeline failed";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatPipelineFailed(anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForPipeline("http://gitlab/pipeline/1")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        verify(messageFormatter).formatPipelineFailed("Test Project", "branch_name");
        verify(messageFormatter).buttonsForPipeline("http://gitlab/pipeline/1");
    }

    @Test
    void formatMessageForEvent_ShouldHandleSuccessfulDeployPipeline() throws JsonProcessingException {
        // Given
        PipelineEvent event = createBasicPipelineEvent();
        event.getPipeline().setStatus("success");

        // Add deploy stage
        PipelineEvent.Stages deployStage = new PipelineEvent.Stages();
        deployStage.setStage("deploy");
        event.setStages(List.of(deployStage));

        String payload = objectMapper.writeValueAsString(event);

        String expectedMessage = "Pipeline deployed";
        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.formatPipelineDeployed(anyString(), anyString())).thenReturn(expectedMessage);
        when(messageFormatter.buttonsForPipeline("http://gitlab/pipeline/1")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(0).getUserRole()).isEqualTo(UserRole.LEAD);
        assertThat(result.get(1).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(1).getUserRole()).isEqualTo(UserRole.PM);
        assertThat(result.get(2).getGitlabUserReceiverId()).isNull();
        assertThat(result.get(2).getUserRole()).isEqualTo(UserRole.DEV);
        verify(messageFormatter).formatPipelineDeployed("Test Project", "branch_name");
        verify(messageFormatter).buttonsForPipeline("http://gitlab/pipeline/1");
    }

    @Test
    void formatMessageForEvent_ShouldNotHandleSuccessfulNonDeployPipeline() throws JsonProcessingException {
        // Given
        PipelineEvent event = createBasicPipelineEvent();
        event.getPipeline().setStatus("success");

        // Add non-deploy stage
        PipelineEvent.Stages buildStage = new PipelineEvent.Stages();
        buildStage.setStage("build");
        event.setStages(List.of(buildStage));

        String payload = objectMapper.writeValueAsString(event);

        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.buttonsForPipeline("http://gitlab/pipeline/1")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
        verify(messageFormatter, never()).formatPipelineDeployed(anyString(), anyString());
        verify(messageFormatter).buttonsForPipeline("http://gitlab/pipeline/1");
    }

    @Test
    void formatMessageForEvent_ShouldNotHandleOtherPipelineStatuses() throws JsonProcessingException {
        // Given
        PipelineEvent event = createBasicPipelineEvent();
        event.getPipeline().setStatus("running");

        String payload = objectMapper.writeValueAsString(event);

        List<InlineKeyboardButtonRow> keyboard = createMockKeyboard();
        when(messageFormatter.buttonsForPipeline("http://gitlab/pipeline/1")).thenReturn(keyboard);

        // When
        List<HandledEvent> result = handler.formatMessageForEvent(payload);

        // Then
        assertThat(result).isEmpty();
        verify(messageFormatter, never()).formatPipelineFailed(anyString(), anyString());
        verify(messageFormatter, never()).formatPipelineDeployed(anyString(), anyString());
        verify(messageFormatter).buttonsForPipeline("http://gitlab/pipeline/1");
    }

    @Test
    void formatMessageForEvent_ShouldHandleInvalidJson() {
        // Given
        String invalidPayload = "invalid json";

        // When & Then
        assertThatThrownBy(() -> handler.formatMessageForEvent(invalidPayload))
                .isInstanceOf(JsonProcessingException.class);
    }

    private PipelineEvent createBasicPipelineEvent() {
        PipelineEvent event = new PipelineEvent();

        User actionUser = new User();
        actionUser.setId(1L);
        actionUser.setName("Test user");
        event.setUser(actionUser);

        Project project = new Project();
        project.setName("Test Project");
        event.setProject(project);

        Pipeline pipeline = new Pipeline();
        pipeline.setId(1L);
        pipeline.setStatus("failed");
        pipeline.setUrl("http://gitlab/pipeline/1");
        pipeline.setRef("branch_name");
        event.setPipeline(pipeline);

        // Default stages - empty list
        event.setStages(new ArrayList<>());

        return event;
    }

    private List<InlineKeyboardButtonRow> createMockKeyboard() {
        InlineKeyboardButtonRow.InlineKeyboardButton button =
                InlineKeyboardButtonRow.InlineKeyboardButton.builder()
                        .text("View Pipeline")
                        .url("http://gitlab/pipeline/1")
                        .build();

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(List.of(button));
        return List.of(row);
    }
}