package ru.z3r0ing.gitlabnotificator.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class TelegramConfig {

    private final AppProperties appProperties;

    @Bean
    public TelegramClient telegramClient() {
        return new OkHttpTelegramClient(appProperties.getTelegram().getBotToken());
    }

}
