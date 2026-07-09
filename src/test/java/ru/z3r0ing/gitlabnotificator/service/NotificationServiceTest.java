package ru.z3r0ing.gitlabnotificator.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private TelegramService telegramService;
    @Mock
    private UserMappingRepository userMappingRepository;
    @InjectMocks
    private NotificationService notificationService;

    @Test
    void send_WithUserReceiver_ShouldSendToSpecificUser() {
        HandledEvent handledEvent = new HandledEvent(100L,
                new MessageWithKeyboard("test", Collections.emptyList()));
        UserMapping userMapping = new UserMapping(1L, 200L, 100L, UserRole.DEV);
        when(userMappingRepository.findByGitlabUserId(100L)).thenReturn(Optional.of(userMapping));

        notificationService.send(handledEvent);

        verify(telegramService, times(1)).sendMarkdownMessage(eq(200L), eq("test"), anyList());
    }

    @Test
    void send_UserMappingNotFound_ShouldNotSend() {
        HandledEvent handledEvent = new HandledEvent(999L,
                new MessageWithKeyboard("test", Collections.emptyList()));
        when(userMappingRepository.findByGitlabUserId(999L)).thenReturn(Optional.empty());

        notificationService.send(handledEvent);

        verify(telegramService, never()).sendMarkdownMessage(anyLong(), anyString(), anyList());
    }

    @Test
    void send_WithRoleReceiver_ShouldSendToAllUsersOfRole() {
        HandledEvent handledEvent = new HandledEvent(UserRole.LEAD,
                new MessageWithKeyboard("test", Collections.emptyList()));
        when(userMappingRepository.findAllByRole(UserRole.LEAD)).thenReturn(Arrays.asList(
                new UserMapping(1L, 100L, 200L, UserRole.LEAD),
                new UserMapping(2L, 101L, 201L, UserRole.LEAD)));

        notificationService.send(handledEvent);

        verify(telegramService, times(1)).sendMarkdownMessage(eq(100L), eq("test"), anyList());
        verify(telegramService, times(1)).sendMarkdownMessage(eq(101L), eq("test"), anyList());
    }

    @Test
    void send_WithoutReceiverAndRole_ShouldThrow() {
        HandledEvent handledEvent = HandledEvent.builder()
                .messageWithKeyboard(new MessageWithKeyboard("test", Collections.emptyList()))
                .build();

        assertThatThrownBy(() -> notificationService.send(handledEvent))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
