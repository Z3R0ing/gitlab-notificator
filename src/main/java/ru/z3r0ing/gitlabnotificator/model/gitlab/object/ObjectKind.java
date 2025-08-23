package ru.z3r0ing.gitlabnotificator.model.gitlab.object;

import lombok.Getter;

@Getter
public enum ObjectKind {
    MERGE_REQUEST("merge_request"),
    NOTE("note"),
    PIPELINE("pipeline"),
    ISSUE("issue"),
    TAG_PUSH("tag_push");

    private final String objectKind;

    ObjectKind(String objectKind) {
        this.objectKind = objectKind;
    }
}
