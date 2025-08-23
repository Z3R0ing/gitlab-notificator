package ru.z3r0ing.gitlabnotificator.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.z3r0ing.gitlabnotificator.config.AppProperties;
import ru.z3r0ing.gitlabnotificator.service.GitlabEventService;

@RestController
@RequestMapping("/webhook/gitlab")
@RequiredArgsConstructor
@Slf4j
public class GitlabWebhookController {

    private final AppProperties appProperties;
    private final GitlabEventService gitlabEventService;

    @PostMapping
    public ResponseEntity<String> handleGitlabWebhook(
            @RequestHeader(value = "X-Gitlab-Token", required = false) String token,
            @RequestHeader(value = "X-Gitlab-Event", required = false) String eventType,
            @RequestBody(required = false) String payload) {

        if (token == null || !token.equals(appProperties.getGitlab().getWebhookSecret())) {
            log.warn("Got webhook request with incorrect or empty token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.info("Got GitLab webhook: {}", eventType);
        if (log.isDebugEnabled()) {
            log.debug("GitLab webhook payload: {}", payload);
        }

        gitlabEventService.handleEvent(eventType, payload);

        return ResponseEntity.ok().build();
    }
}
