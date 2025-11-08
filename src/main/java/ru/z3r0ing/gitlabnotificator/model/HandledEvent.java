package ru.z3r0ing.gitlabnotificator.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.lang.Nullable;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;

@Data
@Builder
@AllArgsConstructor
public class HandledEvent {
    @Nullable
    Long gitlabUserReceiverId;

    @Nullable
    UserRole userRole;

    MessageWithKeyboard messageWithKeyboard;

    public HandledEvent(@Nullable Long gitlabUserReceiverId, MessageWithKeyboard messageWithKeyboard) {
        this.gitlabUserReceiverId = gitlabUserReceiverId;
        this.messageWithKeyboard = messageWithKeyboard;
    }

    public HandledEvent(@Nullable UserRole userRole, MessageWithKeyboard messageWithKeyboard) {
        this.userRole = userRole;
        this.messageWithKeyboard = messageWithKeyboard;
    }
}
