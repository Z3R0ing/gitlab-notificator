package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.IssueEvent;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handler for processing issue events from GitLab webhook.
 * Handles new issue creation events and generates impersonal notifications.
 */
@Component
@RequiredArgsConstructor
public class IssueEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MessageFormatter messageFormatter;

    @Override
    public List<HandledEvent> handleEvent(String payload) throws JsonProcessingException {
        IssueEvent issueEvent = mapper.readValue(payload, IssueEvent.class);
        ru.z3r0ing.gitlabnotificator.model.gitlab.object.Issue issue = issueEvent.getIssue();

        // Skip processing if issue is closed
        if ("closed".equalsIgnoreCase(issue.getState())) {
            return Collections.emptyList();
        }

        // Only handle new issue creation events
        if ("open".equalsIgnoreCase(issue.getAction())) {
            String projectName = issueEvent.getProject().getName();
            String issueTitle = issue.getTitle();
            String issueUrl = issue.getUrl();
            String issueAuthor = issueEvent.getUser().getName();

            String message = messageFormatter.formatNewIssue(projectName, issueTitle, issueAuthor);
            List<InlineKeyboardButtonRow> keyboard = messageFormatter.buttonsForIssue(issueUrl);

            // create notification for LEAD and PM
            List<HandledEvent> handledEvents = new ArrayList<>();
            MessageWithKeyboard messageWithKeyboard = new MessageWithKeyboard(message, keyboard);
            handledEvents.add(new HandledEvent(UserRole.LEAD, messageWithKeyboard));
            handledEvents.add(new HandledEvent(UserRole.PM, messageWithKeyboard));
            return handledEvents;
        }

        return Collections.emptyList();
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.ISSUE.equals(eventType);
    }
}