package ru.z3r0ing.gitlabnotificator.util;

import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;

import java.util.Collections;
import java.util.List;

@Component
public class MessageFormatter {

    /**
     * Format message for approved MR
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @param approverName name of the user who approves
     * @return formatted message text
     */
    public String formatMrApproved(String projectName, String mrTitle, String approverName) {
        return String.format("""
                        ✅ *Merge Request approved!*

                        Project: _%s_
                        MR: *%s*
                        Approved by: %s
                        """,
                projectName, mrTitle, approverName);
    }

    /**
     * Создает inline-клавиатуру с кнопкой для открытия MR
     *
     * @param url URL merge request
     * @return список строк с кнопками
     */
    public List<InlineKeyboardButtonRow> buttonsForMr(String url) {
        if (url == null || url.isEmpty()) {
            return Collections.emptyList();
        }

        InlineKeyboardButtonRow.InlineKeyboardButton button =
                new InlineKeyboardButtonRow.InlineKeyboardButton("🔗 Open MR", url);

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(Collections.singletonList(button));

        return Collections.singletonList(row);
    }
}
