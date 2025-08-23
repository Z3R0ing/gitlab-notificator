package ru.z3r0ing.gitlabnotificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;

@Data
@Builder
@AllArgsConstructor
public class HandledEvent {
    @Nullable
    Long gitlabUserReceiverId;
    MessageWithKeyboard messageWithKeyboard;
}
