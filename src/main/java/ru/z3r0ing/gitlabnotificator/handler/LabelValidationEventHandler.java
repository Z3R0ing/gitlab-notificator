package ru.z3r0ing.gitlabnotificator.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.HandledEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.EventType;
import ru.z3r0ing.gitlabnotificator.model.gitlab.event.IssueEvent;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.Label;
import ru.z3r0ing.gitlabnotificator.model.gitlab.object.User;
import ru.z3r0ing.gitlabnotificator.validation.model.IssueSnapshot;
import ru.z3r0ing.gitlabnotificator.validation.scheduling.IssueValidationScheduler;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Thin inbound adapter of the label validation subsystem.
 * Parses issue events into snapshots and hands them to the scheduler.
 * Never sends notifications itself (they are deferred past the debounce window).
 */
@Component
@ConditionalOnProperty(prefix = "label-rules", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class LabelValidationEventHandler implements EventHandler {
    private static final ObjectMapper mapper = new ObjectMapper();

    private final IssueValidationScheduler scheduler;

    @Override
    public List<HandledEvent> handleEvent(String payload) throws JsonProcessingException {
        IssueEvent issueEvent = mapper.readValue(payload, IssueEvent.class);
        if (issueEvent.getIssue() == null || issueEvent.getProject() == null) {
            log.warn("Issue event without issue or project attributes, skipping label validation");
            return Collections.emptyList();
        }
        scheduler.onIssueEvent(toSnapshot(issueEvent));
        return Collections.emptyList();
    }

    private IssueSnapshot toSnapshot(IssueEvent issueEvent) {
        List<String> labels = issueEvent.getLabels() == null ? Collections.emptyList()
                : issueEvent.getLabels().stream()
                        .map(Label::getTitle)
                        .filter(Objects::nonNull)
                        .toList();
        User actor = issueEvent.getUser();
        // closed issues are not validated: map onto the close action
        // so the scheduler clears any lingering cycle
        String action = "closed".equalsIgnoreCase(issueEvent.getIssue().getState())
                ? "close"
                : issueEvent.getIssue().getAction();
        return new IssueSnapshot(
                issueEvent.getProject().getId(),
                issueEvent.getIssue().getIid(),
                issueEvent.getProject().getName(),
                issueEvent.getIssue().getTitle(),
                issueEvent.getIssue().getUrl(),
                labels,
                actor != null ? actor.getId() : null,
                actor != null ? actor.getName() : null,
                action);
    }

    @Override
    public boolean doesSupportSuchEvent(EventType eventType) {
        return EventType.ISSUE.equals(eventType);
    }
}
