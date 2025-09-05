package ru.z3r0ing.gitlabnotificator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import ru.z3r0ing.gitlabnotificator.handler.EventHandler;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitlabEventService {

    private final TelegramService telegramService;
    private final UserMappingRepository userMappingRepository;
    private final ApplicationContext applicationContext;

    public void handleEvent(String eventTypeRaw, String payload) {
        EventType eventType;
        try {
            eventType = EventType.fromRequestHeader(eventTypeRaw);
        } catch (IllegalArgumentException iae) {
            log.warn("Unsupported GitLab event: {}", eventTypeRaw);
            log.debug("Unsupported GitLab webhook payload: {}", payload);
            return;
        }

        List<EventHandler> eventHandlers = getAllEventHandlers();

        for (EventHandler eventHandler : eventHandlers) {
            if (eventHandler.doesSupportSuchEvent(eventType)) {
                try {
                    List<HandledEvent> handledEventList = eventHandler.formatMessageForEvent(payload);
                    handledEventList.forEach(this::sendEventNotification);
                } catch (JsonProcessingException e) {
                    log.error("Error processing GitLab event payload for event type: {}", eventType, e);
                    log.debug("Bad GitLab webhook payload: {}", payload);
                }
                break;
            }
        }
    }

    private void sendEventNotification(HandledEvent handledEvent) {
        Long gitlabUserReceiverId = handledEvent.getGitlabUserReceiverId();
        if (gitlabUserReceiverId == null) {
            UserRole userRole = handledEvent.getUserRole();
            if (userRole == null) {
                throw new IllegalArgumentException("Need at least 'userRole' or 'gitlabUserReceiverId'");
            }
            List<UserMapping> allByRole = userMappingRepository.findAllByRole(userRole);
            for (UserMapping user : allByRole) {
                telegramService.sendMarkdownMessage(user.getTelegramId(),
                        handledEvent.getMessageWithKeyboard().getMessage(),
                        handledEvent.getMessageWithKeyboard().getKeyboard());
            }
        } else {
            Optional<UserMapping> optionalUser =
                    userMappingRepository.findByGitlabUserId(gitlabUserReceiverId);
            if (optionalUser.isEmpty()) {
                log.warn("User mapping not found for GitLab user ID: {}", gitlabUserReceiverId);
                return;
            }
            UserMapping user = optionalUser.get();
            telegramService.sendMarkdownMessage(user.getTelegramId(),
                    handledEvent.getMessageWithKeyboard().getMessage(),
                    handledEvent.getMessageWithKeyboard().getKeyboard());
        }
    }



    private List<EventHandler> getAllEventHandlers() {
        Map<String, EventHandler> beans = applicationContext.getBeansOfType(EventHandler.class);
        return new ArrayList<>(beans.values());
    }
}
