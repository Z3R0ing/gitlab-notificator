package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.TagPushEvent;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.Collections;
import java.util.List;

/**
 * Handler for processing tag push events from GitLab webhook.
 * Handles new tag creation events and generates impersonal notifications.
 */
@Component
@RequiredArgsConstructor
public class TagPushEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MessageFormatter messageFormatter;

    @Override
    public List<HandledEvent> formatMessageForEvent(String payload) throws JsonProcessingException {
        TagPushEvent tagPushEvent = mapper.readValue(payload, TagPushEvent.class);

        String projectName = tagPushEvent.getProject().getName();
        String tagName = tagPushEvent.getTagName();
        String tagUrl = tagPushEvent.getUrl();

        String message = messageFormatter.formatNewTag(projectName, tagName);
        List<InlineKeyboardButtonRow> keyboard = messageFormatter.buttonsForTag(tagUrl);

        return Collections.singletonList(new HandledEvent(null,
                new MessageWithKeyboard(message, keyboard)));
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.TAG_PUSH.equals(eventType);
    }
}