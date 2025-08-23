package ru.z3r0ing.gitlabnotificator.model.gitlab.event;

import com.fasterxml.jackson.annotation.JsonIgnore;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.ObjectKind;

public abstract class AbstractEvent {

    protected abstract String getObjectKindRaw();

    @JsonIgnore
    protected abstract EventType getEventType();

    @JsonIgnore
    public ObjectKind getObjectKind() {
        return ObjectKind.valueOf(getObjectKindRaw());
    }

}
