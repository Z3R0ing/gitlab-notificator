package ru.z3r0ing.gitlabnotificator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormatterTest {

    private MessageFormatter messageFormatter;

    @BeforeEach
    void setUp() {
        messageFormatter = new MessageFormatter();
    }

    @Test
    void formatMrApproved_shouldReturnFormattedMessageWithAllFields() {
        // Given
        String projectName = "MyProject";
        String mrTitle = "Fix bug in authentication";
        String approverName = "John Doe";

        // When
        String result = messageFormatter.formatMrApproved(projectName, mrTitle, approverName);

        // Then
        assertNotNull(result);
        assertTrue(result.contains("✅ *Merge Request approved!*"));
        assertTrue(result.contains("Project: _MyProject_"));
        assertTrue(result.contains("MR: *Fix bug in authentication*"));
        assertTrue(result.contains("Approved by: John Doe"));
        // Проверяем наличие markdown элементов
        assertTrue(result.contains("*")); // bold
        assertTrue(result.contains("_")); // italic
    }

    @Test
    void buttonsForMr_shouldReturnButtonWithCorrectUrl() {
        // Given
        String url = "https://gitlab.com/project/merge_requests/123";

        // When
        List<InlineKeyboardButtonRow> buttons = messageFormatter.buttonsForMr(url);

        // Then
        assertNotNull(buttons);
        assertEquals(1, buttons.size());
        InlineKeyboardButtonRow row = buttons.get(0);
        assertNotNull(row.getButtons());
        assertEquals(1, row.getButtons().size());
        InlineKeyboardButtonRow.InlineKeyboardButton button = row.getButtons().get(0);
        assertEquals("🔗 Open MR", button.getText());
        assertEquals(url, button.getUrl());
    }

    @Test
    void buttonsForMr_shouldReturnEmptyListWhenUrlIsNull() {
        // When
        List<InlineKeyboardButtonRow> buttons = messageFormatter.buttonsForMr(null);

        // Then
        assertNotNull(buttons);
        assertTrue(buttons.isEmpty());
    }

    @Test
    void buttonsForMr_shouldReturnEmptyListWhenUrlIsEmpty() {
        // When
        List<InlineKeyboardButtonRow> buttons = messageFormatter.buttonsForMr("");

        // Then
        assertNotNull(buttons);
        assertTrue(buttons.isEmpty());
    }
}