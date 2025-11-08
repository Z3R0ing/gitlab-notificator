package ru.z3r0ing.gitlabnotificator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelegramService {

    private final TelegramClient telegramClient;

    public void sendMarkdownMessage(long chatId, String text, List<InlineKeyboardButtonRow> buttons) {
        try {
            SendMessage.SendMessageBuilder messageBuilder = SendMessage.builder()
                    .chatId(String.valueOf(chatId))
                    .text(text)
                    .parseMode("Markdown");

            // Added keyboard if specified
            if (buttons != null && !buttons.isEmpty()) {
                InlineKeyboardMarkup markup = createInlineKeyboardMarkup(buttons);
                messageBuilder.replyMarkup(markup);
            }

            SendMessage message = messageBuilder.build();
            telegramClient.execute(message);
            log.debug("Message send to a chat {}: {}", chatId, text);
        } catch (TelegramApiException e) {
            log.error("Error sending message to a chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private InlineKeyboardMarkup createInlineKeyboardMarkup(List<InlineKeyboardButtonRow> buttonRows) {
        List<InlineKeyboardRow> keyboardRowList = new ArrayList<>();

        for (InlineKeyboardButtonRow row : buttonRows) {
            InlineKeyboardRow inlineKeyboardRow = new InlineKeyboardRow();
            for (InlineKeyboardButtonRow.InlineKeyboardButton button : row.getButtons()) {
                InlineKeyboardButton telegramButton = InlineKeyboardButton.builder()
                        .text(button.getText())
                        .url(button.getUrl())
                        .build();
                inlineKeyboardRow.add(telegramButton);
            }
            keyboardRowList.add(inlineKeyboardRow);
        }

        return new InlineKeyboardMarkup(keyboardRowList);
    }
}
