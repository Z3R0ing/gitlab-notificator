package ru.z3r0ing.gitlabnotificator.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;
import ru.z3r0ing.gitlabnotificator.handler.EventHandler;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.entity.UserMapping;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.repository.UserMappingRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitlabEventServiceTest {

    @Mock
    private TelegramService telegramService;
    @Mock
    private UserMappingRepository userMappingRepository;
    @Mock
    private ApplicationContext applicationContext;
    @InjectMocks
    private GitlabEventService gitlabEventService;

    @Test
    void handleEvent_UnsupportedEventType_ShouldLogWarning() {
        String unsupportedEventType = "UNSUPPORTED_EVENT";
        String payload = "{}";

        gitlabEventService.handleEvent(unsupportedEventType, payload);

        verifyNoInteractions(telegramService);
    }

    @Test
    void handleEvent_SupportedEventType_NoHandlers_ShouldDoNothing() {
        String eventType = EventType.ISSUE.name();
        String payload = "{}";
        when(applicationContext.getBeansOfType(EventHandler.class)).thenReturn(Collections.emptyMap());

        gitlabEventService.handleEvent(eventType, payload);

        verifyNoInteractions(telegramService);
    }

    @Test
    void handleEvent_HandlerThrowsJsonProcessingException_ShouldLogError() throws JsonProcessingException {
        String eventType = EventType.ISSUE.name();
        String payload = "invalid_json";
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(mockHandler.formatMessageForEvent(payload)).thenThrow(JsonProcessingException.class);
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("issueHandler", mockHandler));

        gitlabEventService.handleEvent(eventType, payload);

        verifyNoInteractions(telegramService);
    }

    @Test
    void handleEvent_ValidEventHandler_ShouldSendNotifications() throws JsonProcessingException {
        String eventType = EventType.ISSUE.name();
        String payload = "{}";
        HandledEvent handledEvent = new HandledEvent(null, new MessageWithKeyboard("test", Collections.emptyList()));
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(mockHandler.formatMessageForEvent(payload)).thenReturn(Collections.singletonList(handledEvent));
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("issueHandler", mockHandler));
        when(userMappingRepository.findAllByRole(UserRole.LEAD))
                .thenReturn(Collections.singletonList(new UserMapping(1L, 100L, 200L, UserRole.LEAD)));

        gitlabEventService.handleEvent(eventType, payload);

        verify(telegramService, times(1))
                .sendMarkdownMessage(eq(100L), eq("test"), anyList());
    }

    @Test
    void handleEvent_UserMappingNotFound_ShouldLogWarning() throws JsonProcessingException {
        String eventType = EventType.NOTE.name();
        String payload = "{}";
        HandledEvent handledEvent = new HandledEvent(999L, new MessageWithKeyboard("test", Collections.emptyList()));
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.NOTE)).thenReturn(true);
        when(mockHandler.formatMessageForEvent(payload)).thenReturn(Collections.singletonList(handledEvent));
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("noteHandler", mockHandler));
        when(userMappingRepository.findByGitlabUserId(999L)).thenReturn(Optional.empty());

        gitlabEventService.handleEvent(eventType, payload);

        verify(telegramService, never()).sendMarkdownMessage(anyLong(), anyString(), anyList());
    }

    @Test
    void handleEvent_WithUserReceiver_ShouldSendToSpecificUser() throws JsonProcessingException {
        String eventType = EventType.NOTE.name();
        String payload = "{}";
        HandledEvent handledEvent = new HandledEvent(100L, new MessageWithKeyboard("test", Collections.emptyList()));
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.NOTE)).thenReturn(true);
        when(mockHandler.formatMessageForEvent(payload)).thenReturn(Collections.singletonList(handledEvent));
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("noteHandler", mockHandler));
        UserMapping userMapping = new UserMapping(1L, 200L, 100L, UserRole.DEV);
        when(userMappingRepository.findByGitlabUserId(100L)).thenReturn(Optional.of(userMapping));

        gitlabEventService.handleEvent(eventType, payload);

        verify(telegramService, times(1))
                .sendMarkdownMessage(eq(200L), eq("test"), anyList());
    }

    @Test
    void handleEvent_WithoutUserReceiver_ShouldSendToLeads() throws JsonProcessingException {
        String eventType = EventType.ISSUE.name();
        String payload = "{}";
        HandledEvent handledEvent = new HandledEvent(null, new MessageWithKeyboard("test", Collections.emptyList()));
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(mockHandler.formatMessageForEvent(payload)).thenReturn(Collections.singletonList(handledEvent));
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("issueHandler", mockHandler));
        List<UserMapping> leads = Arrays.asList(
                new UserMapping(1L, 100L, 200L, UserRole.LEAD),
                new UserMapping(2L, 101L, 201L, UserRole.LEAD)
        );
        when(userMappingRepository.findAllByRole(UserRole.LEAD)).thenReturn(leads);

        gitlabEventService.handleEvent(eventType, payload);

        verify(telegramService, times(1))
                .sendMarkdownMessage(eq(100L), eq("test"), anyList());
        verify(telegramService, times(1))
                .sendMarkdownMessage(eq(101L), eq("test"), anyList());
    }
}