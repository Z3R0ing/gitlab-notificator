package ru.z3r0ing.gitlabnotificator.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;

import java.util.List;
import java.util.Optional;

/**
 * Resolves the recipient of a handled event and delivers it via Telegram.
 * Recipient is either a specific GitLab user or every user with a role.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final TelegramService telegramService;
    private final UserMappingRepository userMappingRepository;

    /**
     * Sends the notification to the recipient described by the handled event.
     *
     * @param handledEvent event with message and either gitlabUserReceiverId or userRole set
     * @throws IllegalArgumentException if neither receiver id nor role is set
     */
    public void send(HandledEvent handledEvent) {
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
}
