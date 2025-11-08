package ru.z3r0ing.gitlabnotificator.model.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class MessageWithKeyboard {
    private String message;

    private List<InlineKeyboardButtonRow> keyboard;
}
