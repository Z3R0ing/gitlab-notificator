package ru.z3r0ing.gitlabnotificator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.z3r0ing.gitlabnotificator.handler.EventHandler;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitlabEventService {

    private final NotificationService notificationService;
    private final ApplicationContext applicationContext;

    public void handleGitlabEvent(String eventTypeRaw, String payload) {
        EventType eventType;
        try {
            eventType = EventType.fromRequestHeader(eventTypeRaw);
        } catch (IllegalArgumentException iae) {
            log.warn("Unsupported GitLab event: {}", eventTypeRaw);
            log.debug("Unsupported GitLab webhook payload: {}", payload);
            return;
        }

        List<EventHandler> eventHandlers = getAllEventHandlers();

        for (EventHandler eventHandler : eventHandlers) {
            if (!eventHandler.doesSupportSuchEvent(eventType)) {
                continue;
            }
            try {
                List<HandledEvent> handledEventList = eventHandler.handleEvent(payload);
                handledEventList.forEach(notificationService::send);
            } catch (JsonProcessingException | RuntimeException e) {
                log.error("Error processing GitLab event of type {} in handler {}",
                        eventType, eventHandler.getClass().getSimpleName(), e);
                log.debug("Bad GitLab webhook payload: {}", payload);
            }
        }
    }

    private List<EventHandler> getAllEventHandlers() {
        Map<String, EventHandler> beans = applicationContext.getBeansOfType(EventHandler.class);
        return new ArrayList<>(beans.values());
    }
}
