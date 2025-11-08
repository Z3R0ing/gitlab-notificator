package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;

import java.util.List;

public interface EventHandler {

    /**
     * Formats messages based on the merge request event type.
     *
     * @param payload JSON string containing the merge request event data
     * @return List of HandledEvent objects containing formatted messages and recipient information
     * @throws JsonProcessingException if payload cannot be parsed
     */
    List<HandledEvent> handleEvent(String payload) throws JsonProcessingException;

    /**
     * Checks if this handler supports the given event type.
     *
     * @param eventType the type of event to check
     * @return true if this handler supports MERGE_REQUEST events, false otherwise
     */
    boolean doesSupportSuchEvent(EventType eventType);

}
