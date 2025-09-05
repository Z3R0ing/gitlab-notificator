package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.model.UserRole;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.MergeRequestEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handler for processing merge request events from GitLab webhook.
 * Handles various MR events like creation, draft removal, reviewer assignment,
 * approved and merging.
 */
@Component
@RequiredArgsConstructor
public class MergeRequestEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final MessageFormatter messageFormatter;

    @Override
    public List<HandledEvent> formatMessageForEvent(String payload) throws JsonProcessingException {
        MergeRequestEvent mergeRequestEvent = mapper.readValue(payload, MergeRequestEvent.class);
        MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();

        // If MR is closed, no notifications needed
        if (isClosedMergeRequest(mergeRequest)) {
            return Collections.emptyList();
        }

        return processMergeRequestEvent(mergeRequestEvent);
    }

    /**
     * Checks if the merge request is closed.
     *
     * @param mergeRequest the merge request to check
     * @return true if merge request is closed, false otherwise
     */
    private boolean isClosedMergeRequest(MergeRequest mergeRequest) {
        return "closed".equalsIgnoreCase(mergeRequest.getState());
    }

    /**
     * Processes the merge request event and generates appropriate notifications.
     *
     * @param mergeRequestEvent the event to process
     * @return list of handled events with formatted messages
     */
    private List<HandledEvent> processMergeRequestEvent(MergeRequestEvent mergeRequestEvent) {
        List<HandledEvent> handledEventList = new ArrayList<>();
        MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();

        // Create keyboard with link to the merge request
        List<InlineKeyboardButtonRow> keyboard = messageFormatter.buttonsForMr(mergeRequest.getUrl());

        // Process different types of events
        handledEventList.addAll(handleOpenAction(mergeRequestEvent, keyboard));
        handledEventList.addAll(handleDraftRemoval(mergeRequestEvent, keyboard));
        handledEventList.addAll(handleReviewerAssignment(mergeRequestEvent, keyboard));
        handledEventList.addAll(handleApprovedAction(mergeRequestEvent, keyboard));
        handledEventList.addAll(handleMergeAction(mergeRequestEvent, keyboard));

        return handledEventList;
    }

    /**
     * Handles new merge request creation.
     *
     * @param mergeRequestEvent the event
     * @param keyboard          keyboard with merge request link
     */
    private List<HandledEvent> handleOpenAction(MergeRequestEvent mergeRequestEvent, List<InlineKeyboardButtonRow> keyboard) {
        MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();
        if ("open".equalsIgnoreCase(mergeRequest.getAction())) {
            String projectName = mergeRequestEvent.getProject().getName();
            String mergeRequestTitle = mergeRequest.getTitle();
            User actionUser = mergeRequestEvent.getUser();
            String newMrMessage = messageFormatter.formatNewMr(
                    projectName,
                    mergeRequestTitle,
                    actionUser.getName()
            );
            // Create notification for LEAD
            return Collections.singletonList(new HandledEvent(UserRole.LEAD,
                    new MessageWithKeyboard(newMrMessage, keyboard)));
        }
        return Collections.emptyList();
    }

    /**
     * Handles draft status removal.
     *
     * @param mergeRequestEvent the event
     * @param keyboard          keyboard with merge request link
     */
    private List<HandledEvent> handleDraftRemoval(MergeRequestEvent mergeRequestEvent, List<InlineKeyboardButtonRow> keyboard) {
        MergeRequestEvent.Changes eventChanges = mergeRequestEvent.getChanges();
        if (eventChanges != null && eventChanges.getDraft() != null) {
            if (!eventChanges.getDraft().getCurrent()) {
                MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();
                String projectName = mergeRequestEvent.getProject().getName();
                String mergeRequestTitle = mergeRequest.getTitle();
                String mrUndraftMessage = messageFormatter.formatMrUndraft(
                        projectName,
                        mergeRequestTitle
                );
                // Create notification for LEAD
                return Collections.singletonList(new HandledEvent(UserRole.LEAD,
                        new MessageWithKeyboard(mrUndraftMessage, keyboard)));
            }
        }
        return Collections.emptyList();
    }

    /**
     * Handles new reviewer assignments - notify each reviewer individually.
     *
     * @param mergeRequestEvent the event
     * @param keyboard          keyboard with merge request link
     */
    private List<HandledEvent> handleReviewerAssignment(MergeRequestEvent mergeRequestEvent, List<InlineKeyboardButtonRow> keyboard) {
        List<User> newReviewers = getMergeRequestNewReviewers(mergeRequestEvent);
        if (!CollectionUtils.isEmpty(newReviewers)) {
            List<HandledEvent> handledEventList = new ArrayList<>();
            User actionUser = mergeRequestEvent.getUser();
            for (User reviewer : newReviewers) {
                // skip if reviewer is action user
                if (reviewer.getId().equals(actionUser.getId())) {
                    continue;
                }

                MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();
                String projectName = mergeRequestEvent.getProject().getName();
                String mergeRequestTitle = mergeRequest.getTitle();
                String reviewerMessage = messageFormatter.formatYouAreMrReviewerNow(
                        projectName,
                        mergeRequestTitle
                );

                handledEventList.add(new HandledEvent(reviewer.getId(),
                        new MessageWithKeyboard(reviewerMessage, keyboard)));
            }
            return handledEventList;
        }
        return Collections.emptyList();
    }

    /**
     * Handles MR approved - notify assignee and send impersonal notification.
     *
     * @param mergeRequestEvent the event
     * @param keyboard          keyboard with merge request link
     */
    private List<HandledEvent> handleApprovedAction(MergeRequestEvent mergeRequestEvent, List<InlineKeyboardButtonRow> keyboard) {
        MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();
        if ("approved".equalsIgnoreCase(mergeRequest.getAction())) {
            List<HandledEvent> handledEventList = new ArrayList<>();

            String projectName = mergeRequestEvent.getProject().getName();
            String mergeRequestTitle = mergeRequest.getTitle();
            User actionUser = mergeRequestEvent.getUser();
            Long assigneeId = mergeRequest.getAssigneeId();

            String mrApprovedMessage = messageFormatter.formatMrApproved(
                    projectName,
                    mergeRequestTitle,
                    actionUser.getName()
            );

            // Notify assignee if exists and not action user
            if (assigneeId != null && !actionUser.getId().equals(assigneeId)) {
                handledEventList.add(new HandledEvent(assigneeId,
                        new MessageWithKeyboard(mrApprovedMessage, keyboard)));
            }

            // Create notification for LEAD
            handledEventList.add(new HandledEvent(UserRole.LEAD,
                    new MessageWithKeyboard(mrApprovedMessage, keyboard)));

            return handledEventList;
        }
        return Collections.emptyList();
    }

    /**
     * Handles MR merge - notify assignee and send impersonal notification.
     *
     * @param mergeRequestEvent the event
     * @param keyboard          keyboard with merge request link
     */
    private List<HandledEvent> handleMergeAction(MergeRequestEvent mergeRequestEvent, List<InlineKeyboardButtonRow> keyboard) {
        MergeRequest mergeRequest = mergeRequestEvent.getMergeRequest();
        if ("merge".equalsIgnoreCase(mergeRequest.getAction())) {
            List<HandledEvent> handledEventList = new ArrayList<>();

            String projectName = mergeRequestEvent.getProject().getName();
            String mergeRequestTitle = mergeRequest.getTitle();
            User actionUser = mergeRequestEvent.getUser();
            Long assigneeId = mergeRequest.getAssigneeId();

            String mrMergedMessage = messageFormatter.formatMrMerged(
                    projectName,
                    mergeRequestTitle,
                    actionUser.getName()
            );

            MessageWithKeyboard messageWithKeyboard = new MessageWithKeyboard(mrMergedMessage, keyboard);

            // Notify assignee if exists and not action user
            if (assigneeId != null && !actionUser.getId().equals(assigneeId)) {
                handledEventList.add(new HandledEvent(assigneeId, messageWithKeyboard));
            }

            // Create notification for LEAD and PM
            handledEventList.add(new HandledEvent(UserRole.LEAD, messageWithKeyboard));
            handledEventList.add(new HandledEvent(UserRole.PM, messageWithKeyboard));

            return handledEventList;
        }
        return Collections.emptyList();
    }

    private List<User> getMergeRequestNewReviewers(MergeRequestEvent mergeRequestEvent) {
        if ("open".equals(mergeRequestEvent.getMergeRequest().getAction())
                && mergeRequestEvent.getReviewers() != null) {
            return mergeRequestEvent.getReviewers();
        }
        if (mergeRequestEvent.getChanges() != null
                && mergeRequestEvent.getChanges().getReviewers() != null) {
            return mergeRequestEvent.getChanges().getReviewers().getCurrent();
        }
        return Collections.emptyList();
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.MERGE_REQUEST.equals(eventType);
    }
}
