package ru.z3r0ing.gitlabnotificator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@Service
public class UpdateConsumerService implements LongPollingSingleThreadUpdateConsumer {

    @Override
    public void consume(Update update) {
        // not needed now
        log.info("Got update from Telegram chat {}", update.getUpdateId());
    }

}
