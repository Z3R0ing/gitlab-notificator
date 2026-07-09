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
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitlabEventServiceTest {

    @Mock
    private NotificationService notificationService;
    @Mock
    private ApplicationContext applicationContext;
    @InjectMocks
    private GitlabEventService gitlabEventService;

    @Test
    void handleGitlabEvent_UnsupportedGitlabEventType_ShouldLogWarning() {
        gitlabEventService.handleGitlabEvent("UNSUPPORTED_EVENT", "{}");

        verifyNoInteractions(notificationService);
    }

    @Test
    void handleGitlabEvent_SupportedGitlabEventType_NoHandlers_ShouldDoNothing() {
        when(applicationContext.getBeansOfType(EventHandler.class)).thenReturn(Collections.emptyMap());

        gitlabEventService.handleGitlabEvent(EventType.ISSUE.getRequestHeader(), "{}");

        verifyNoInteractions(notificationService);
    }

    @Test
    void handleGitlabEvent_HandlerThrowsJsonProcessingException_ShouldLogError() throws JsonProcessingException {
        String payload = "invalid_json";
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(mockHandler.handleEvent(payload)).thenThrow(JsonProcessingException.class);
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("issueHandler", mockHandler));

        gitlabEventService.handleGitlabEvent(EventType.ISSUE.getRequestHeader(), payload);

        verifyNoInteractions(notificationService);
    }

    @Test
    void handleGitlabEvent_ValidGitlabEventHandler_ShouldSendNotifications() throws JsonProcessingException {
        String payload = "{}";
        HandledEvent handledEvent = new HandledEvent(UserRole.LEAD,
                new MessageWithKeyboard("test", Collections.emptyList()));
        EventHandler mockHandler = mock(EventHandler.class);
        when(mockHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(mockHandler.handleEvent(payload)).thenReturn(Collections.singletonList(handledEvent));
        when(applicationContext.getBeansOfType(EventHandler.class))
                .thenReturn(Collections.singletonMap("issueHandler", mockHandler));

        gitlabEventService.handleGitlabEvent(EventType.ISSUE.getRequestHeader(), payload);

        verify(notificationService, times(1)).send(handledEvent);
    }

    @Test
    void handleGitlabEvent_TwoHandlersSupportSameType_ShouldInvokeBoth() throws JsonProcessingException {
        String payload = "{}";
        EventHandler firstHandler = mock(EventHandler.class);
        EventHandler secondHandler = mock(EventHandler.class);
        when(firstHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(secondHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(firstHandler.handleEvent(payload)).thenReturn(Collections.emptyList());
        when(secondHandler.handleEvent(payload)).thenReturn(Collections.emptyList());
        Map<String, EventHandler> handlers = new LinkedHashMap<>();
        handlers.put("first", firstHandler);
        handlers.put("second", secondHandler);
        when(applicationContext.getBeansOfType(EventHandler.class)).thenReturn(handlers);

        gitlabEventService.handleGitlabEvent(EventType.ISSUE.getRequestHeader(), payload);

        verify(firstHandler, times(1)).handleEvent(payload);
        verify(secondHandler, times(1)).handleEvent(payload);
    }

    @Test
    void handleGitlabEvent_FirstHandlerThrowsRuntimeException_ShouldStillInvokeSecond() throws JsonProcessingException {
        String payload = "{}";
        EventHandler firstHandler = mock(EventHandler.class);
        EventHandler secondHandler = mock(EventHandler.class);
        when(firstHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(secondHandler.doesSupportSuchEvent(EventType.ISSUE)).thenReturn(true);
        when(firstHandler.handleEvent(payload)).thenThrow(new IllegalStateException("boom"));
        when(secondHandler.handleEvent(payload)).thenReturn(Collections.emptyList());
        Map<String, EventHandler> handlers = new LinkedHashMap<>();
        handlers.put("first", firstHandler);
        handlers.put("second", secondHandler);
        when(applicationContext.getBeansOfType(EventHandler.class)).thenReturn(handlers);

        gitlabEventService.handleGitlabEvent(EventType.ISSUE.getRequestHeader(), payload);

        verify(secondHandler, times(1)).handleEvent(payload);
    }
}
