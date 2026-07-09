package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;

import java.util.List;

/**
 * Subscriber to GitLab webhook event types.
 * Every handler whose doesSupportSuchEvent() returns true receives the event,
 * independently and in unspecified order. An exception thrown by one handler
 * does not affect the others.
 */
public interface EventHandler {

    /**
     * Processes the raw webhook payload.
     * May have deferred side effects (e.g. scheduling later work).
     *
     * @param payload JSON string with the GitLab event data
     * @return notifications to send immediately; may be empty
     * @throws JsonProcessingException if payload cannot be parsed
     */
    List<HandledEvent> handleEvent(String payload) throws JsonProcessingException;

    /**
     * Checks if this handler supports the given event type.
     *
     * @param eventType the type of event to check
     * @return true if this handler wants to receive events of this type
     */
    boolean doesSupportSuchEvent(EventType eventType);

}
