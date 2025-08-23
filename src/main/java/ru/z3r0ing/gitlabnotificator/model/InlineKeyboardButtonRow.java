package ru.z3r0ing.gitlabnotificator.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InlineKeyboardButtonRow {
    private List<InlineKeyboardButton> buttons;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineKeyboardButton {
        private String text;
        private String url;
    }
}
