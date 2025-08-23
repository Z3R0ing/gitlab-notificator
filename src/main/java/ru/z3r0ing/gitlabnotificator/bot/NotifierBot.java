package ru.z3r0ing.gitlabnotificator.bot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import ru.z3r0ing.gitlabnotificator.config.AppProperties;
import ru.z3r0ing.gitlabnotificator.service.UpdateConsumerService;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotifierBot implements SpringLongPollingBot {

    private final AppProperties appProperties;
    private final UpdateConsumerService updateConsumerService;

    public String getBotUsername() {
        return appProperties.getTelegram().getBotUsername();
    }

    @Override
    public String getBotToken() {
        return appProperties.getTelegram().getBotToken();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return updateConsumerService;
    }
}
