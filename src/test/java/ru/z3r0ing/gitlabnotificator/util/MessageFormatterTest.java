package ru.z3r0ing.gitlabnotificator.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.validation.model.GroupCardinalityViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.UnknownLabelViolation;
import ru.z3r0ing.gitlabnotificator.validation.model.Violation;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void formatLabelViolations_ShouldContainIssueTitleAndAllViolationLines() {
        List<Violation> violations = List.of(
                new GroupCardinalityViolation("Status", List.of("S:Review", "S:Testing"), 1, 1, 2),
                new GroupCardinalityViolation("Type", List.of(), 1, 1, 0),
                new UnknownLabelViolation(List.of("В работе")));

        String message = messageFormatter.formatLabelViolations("smartdebt", "Test issue", violations);

        assertThat(message).contains("Test issue");
        assertThat(message).contains("Status").contains("S:Review").contains("S:Testing");
        assertThat(message).contains("Type");
        assertThat(message).contains("В работе");
        assertThat(message).contains("at most 1 allowed");
        assertThat(message).contains("at least 1 required");
        assertThat(message).contains("Labels outside the system: В работе");
    }

    @Test
    void formatLabelViolationsEscalated_ShouldMentionNotifiedUserAndDelay() {
        List<Violation> violations = List.of(
                new GroupCardinalityViolation("Status", List.of(), 1, 1, 0));

        String message = messageFormatter.formatLabelViolationsEscalated(
                "smartdebt", "Test issue", violations, "Roman Petrov", "1h");

        assertThat(message).contains("Roman Petrov");
        assertThat(message).contains("1h");
        assertThat(message).contains("Status");
        assertThat(message).contains("at least 1 required");
    }
}