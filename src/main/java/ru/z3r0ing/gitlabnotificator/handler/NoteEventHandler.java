package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.NoteEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.MergeRequest;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.model.telegram.InlineKeyboardButtonRow;
import ru.z3r0ing.gitlabnotificator.model.telegram.MessageWithKeyboard;
import ru.z3r0ing.gitlabnotificator.util.MessageFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Handles note/comment events from the source system and formats them for notification.
 * Specifically processes comments on Merge Requests and generates notifications for relevant users.
 */
@Component
@RequiredArgsConstructor
public class NoteEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();
    private final MessageFormatter messageFormatter;

    @Override
    public List<HandledEvent> handleEvent(String payload) throws JsonProcessingException {
        // Parse the JSON payload into a NoteEvent object
        NoteEvent noteEvent = mapper.readValue(payload, NoteEvent.class);

        // Only process comments on Merge Requests, ignore other note types
        if (!"mergerequest".equalsIgnoreCase(noteEvent.getNote().getNoteableType())) {
            return Collections.emptyList();
        }

        // Extract merge request data from the event
        MergeRequest mergeRequest = noteEvent.getMergeRequest();
        if (mergeRequest == null) {
            return Collections.emptyList();
        }

        // Create inline keyboard with link to the comment
        List<InlineKeyboardButtonRow> keyboard = messageFormatter.buttonsForNote(mergeRequest.getUrl());

        // Format the notification message with relevant context
        String projectName = noteEvent.getProject().getName();
        String mergeRequestTitle = mergeRequest.getTitle();
        String authorName = noteEvent.getUser().getName();
        String message = messageFormatter.formatNewCommentForMr(projectName, mergeRequestTitle, authorName);

        // Identify who should receive notifications about this comment
        List<Long> recipients = getNotificationRecipients(mergeRequest, noteEvent.getUser());

        // Create individual notification events for each recipient
        List<HandledEvent> handledEvents = new ArrayList<>();
        for (Long recipientId : recipients) {
            handledEvents.add(new HandledEvent(
                    recipientId,
                    new MessageWithKeyboard(message, keyboard)
            ));
        }

        return handledEvents;
    }

    /**
     * Determines the recipients who should be notified about a new comment.
     * Includes assignee and reviewers of the merge request, excluding the comment author.
     *
     * @param mergeRequest  the merge request that was commented on
     * @param commentAuthor the user who created the comment
     * @return list of Gitlab users ID who should receive notifications
     */
    private List<Long> getNotificationRecipients(MergeRequest mergeRequest, User commentAuthor) {
        List<Long> recipients = new ArrayList<>();

        Long assigneeId = mergeRequest.getAssigneeId();

        // Add assignee if they exist and are not the comment author
        if (assigneeId != null &&
                !assigneeId.equals(commentAuthor.getId())) {
            recipients.add(assigneeId);
        }

        // FIXME Gitlab Webhooks send MR without reviewers for Note Event
        // Add reviewers who are not the comment author
        if (mergeRequest.getReviewers() != null) {
            for (User reviewer : mergeRequest.getReviewers()) {
                if (!reviewer.getId().equals(commentAuthor.getId())) {
                    recipients.add(reviewer.getId());
                }
            }
        }

        return recipients;
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.NOTE.equals(eventType);
    }
}