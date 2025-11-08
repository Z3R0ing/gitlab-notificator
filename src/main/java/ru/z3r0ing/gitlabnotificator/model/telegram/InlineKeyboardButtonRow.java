package ru.z3r0ing.gitlabnotificator.model.telegram;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
public class InlineKeyboardButtonRow {
    private List<InlineKeyboardButton> buttons;

    @Data
    @Builder
    @AllArgsConstructor
    public static class InlineKeyboardButton {
        private String text;
        private String url;
    }
}
