package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum EventType {
    MERGE_REQUEST("Merge Request Hook"),
    NOTE("Note Hook"),
    PIPELINE("Pipeline Hook"),
    ISSUE("Issue Hook"),
    TAG_PUSH("Tag Push Hook");

    private final String requestHeader;

    public static EventType fromRequestHeader(String requestHeader) {
        for (EventType eventType : EventType.values()) {
            if (eventType.getRequestHeader().equals(requestHeader))
                return eventType;
        }
        throw new IllegalArgumentException("No such enum constant " + requestHeader);
    }

}
