package ru.z3r0ing.gitlabnotificator.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@ConfigurationProperties(prefix = "app")
@Validated
@Slf4j
public class AppProperties {

    private final Telegram telegram = new Telegram();
    private final Gitlab gitlab = new Gitlab();

    @Data
    public static class Telegram {
        @NotBlank(message = "Telegram bot username must be provided")
        private String botUsername;

        @NotBlank(message = "Telegram bot token must be provided")
        private String botToken;
    }

    @Data
    public static class Gitlab {
        @NotBlank(message = "Gitlab webhook secret must be provided")
        private String webhookSecret;
    }

    @PostConstruct
    public void validate() {
        if ("CHANGE_ME".equals(telegram.getBotToken())) {
            log.warn("Telegram bot token is not configured! Please set TG_BOT_TOKEN environment variable.");
        }
        if ("CHANGE_ME".equals(gitlab.getWebhookSecret())) {
            log.warn("GitLab webhook secret is not configured! Please set GITLAB_WEBHOOK_SECRET environment variable.");
        }
    }
}