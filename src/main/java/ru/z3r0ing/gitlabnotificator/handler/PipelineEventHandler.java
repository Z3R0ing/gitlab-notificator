package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.PipelineEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handler for processing pipeline events from GitLab webhook.
 * Handles various pipeline events including failed pipelines and successful deploy pipelines.
 */
@Component
@RequiredArgsConstructor
public class PipelineEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final MessageFormatter messageFormatter;

    @Override
    public List<HandledEvent> formatMessageForEvent(String payload) throws JsonProcessingException {
        PipelineEvent pipelineEvent = mapper.readValue(payload, PipelineEvent.class);
        List<HandledEvent> handledEventList = new ArrayList<>();

        String pipelineUrl = pipelineEvent.getPipeline().getUrl();

        // Create keyboard with link to the pipeline
        List<InlineKeyboardButtonRow> keyboard = messageFormatter.buttonsForPipeline(pipelineUrl);

        // Process different types of events
        handledEventList.addAll(handleFailedPipeline(pipelineEvent, keyboard));
        handledEventList.addAll(handleSuccessfulPipeline(pipelineEvent, keyboard));

        return handledEventList;
    }

    /**
     * Handle failed pipeline.
     *
     * @param pipelineEvent the event
     * @param keyboard      keyboard with merge request link
     */
    private List<HandledEvent> handleFailedPipeline(PipelineEvent pipelineEvent, List<InlineKeyboardButtonRow> keyboard) {
        String pipelineStatus = pipelineEvent.getPipeline().getStatus();
        if ("failed".equalsIgnoreCase(pipelineStatus)) {
            String projectName = pipelineEvent.getProject().getName();
            String pipelineName = pipelineEvent.getPipeline().getRef();

            String failedMessage = messageFormatter.formatPipelineFailed(projectName, pipelineName);
            MessageWithKeyboard messageWithKeyboard = new MessageWithKeyboard(failedMessage, keyboard);

            // If pipeline is related to MR, notify action user
            if (pipelineEvent.getMergeRequest() != null) {
                User actionUser = pipelineEvent.getUser();
                return Collections.singletonList(new HandledEvent(actionUser.getId(), messageWithKeyboard));
            } else {
                // If not related to MR, send impersonal notification
                return Collections.singletonList(new HandledEvent(null, messageWithKeyboard));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Handle successful deploy pipeline.
     *
     * @param pipelineEvent the event
     * @param keyboard      keyboard with merge request link
     */
    private List<HandledEvent> handleSuccessfulPipeline(PipelineEvent pipelineEvent, List<InlineKeyboardButtonRow> keyboard) {
        String pipelineStatus = pipelineEvent.getPipeline().getStatus();
        if ("success".equalsIgnoreCase(pipelineStatus) && isDeployPipeline(pipelineEvent)) {
            String projectName = pipelineEvent.getProject().getName();
            String pipelineName = pipelineEvent.getPipeline().getRef();
            String deployedMessage = messageFormatter.formatPipelineDeployed(projectName, pipelineName);
            MessageWithKeyboard messageWithKeyboard = new MessageWithKeyboard(deployedMessage, keyboard);
            return Collections.singletonList(new HandledEvent(null, messageWithKeyboard));
        }
        return Collections.emptyList();
    }

    /**
     * Checks if the pipeline is a deployment pipeline by examining its stages
     *
     * @param pipelineEvent the pipeline event to check
     * @return true if pipeline contains deploy stage, false otherwise
     */
    private boolean isDeployPipeline(PipelineEvent pipelineEvent) {
        return pipelineEvent.getStages().stream()
                .anyMatch(stage -> "deploy".equalsIgnoreCase(stage.getStage()));
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.PIPELINE.equals(eventType);
    }
}