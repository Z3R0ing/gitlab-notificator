package ru.z3r0ing.gitlabnotificator.util;

import org.springframework.stereotype.Component;
import ru.z3r0ing.gitlabnotificator.model.InlineKeyboardButtonRow;

import java.util.Collections;
import java.util.List;

@Component
public class MessageFormatter {

    /**
     * Format message for approved MR
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @param approverName name of the user who approves
     * @return formatted message text
     */
    public String formatMrApproved(String projectName, String mrTitle, String approverName) {
        return String.format("""
                        ✅ *Merge Request approved!*

                        Project: _%s_
                        MR: *%s*
                        Approved by: %s
                        """,
                projectName, mrTitle, approverName);
    }

    /**
     * Format message for assigned MR reviewer
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @return formatted message text
     */
    public String formatYouAreMrReviewerNow(String projectName, String mrTitle) {
        return String.format("""
                        👀 *You are assigned as MR reviewer!*

                        Project: _%s_
                        MR: *%s*
                        Please review this merge request
                        """,
                projectName, mrTitle);
    }

    /**
     * Format message for merged MR
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @param mergerName name of the user who merged
     * @return formatted message text
     */
    public String formatMrMerged(String projectName, String mrTitle, String mergerName) {
        return String.format("""
                        🚀 *Merge Request merged!*

                        Project: _%s_
                        MR: *%s_
                        Merged by: %s
                        """,
                projectName, mrTitle, mergerName);
    }

    /**
     * Format message for MR undrafted
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @return formatted message text
     */
    public String formatMrUndraft(String projectName, String mrTitle) {
        return String.format("""
                        📝 *Merge Request is ready for review!*

                        Project: _%s_
                        MR: *%s*
                        MR was moved from draft status
                        """,
                projectName, mrTitle);
    }

    /**
     * Format message for new MR
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @param mrAuthor author of the merge request
     * @return formatted message text
     */
    public String formatNewMr(String projectName, String mrTitle, String mrAuthor) {
        return String.format("""
                        🆕 *New Merge Request created!*

                        Project: _%s_
                        MR: *%s*
                        Author: %s
                        """,
                projectName, mrTitle, mrAuthor);
    }

    /**
     * Format message for new comment on MR
     *
     * @param projectName project name
     * @param mrTitle merge request title
     * @param commentAuthor author of the comment
     * @return formatted message text
     */
    public String formatNewCommentForMr(String projectName, String mrTitle, String commentAuthor) {
        return String.format("""
                        💬 *New comment on Merge Request!*

                        Project: _%s_
                        MR: *%s*
                        Comment by: %s
                        """,
                projectName, mrTitle, commentAuthor);
    }

    /**
     * Format message for new issue
     *
     * @param projectName project name
     * @param issueTitle issue title
     * @param issueAuthor author of the issue
     * @return formatted message text
     */
    public String formatNewIssue(String projectName, String issueTitle, String issueAuthor) {
        return String.format("""
                        🐛 *New Issue created!*

                        Project: _%s_
                        Issue: *%s*
                        Author: %s
                        """,
                projectName, issueTitle, issueAuthor);
    }

    /**
     * Format message for new tag
     *
     * @param projectName project name
     * @param tagName tag name
     * @return formatted message text
     */
    public String formatNewTag(String projectName, String tagName) {
        return String.format("""
                        🏷️ *New Tag created!*

                        Project: _%s_
                        Tag: *%s*
                        """,
                projectName, tagName);
    }

    /**
     * Format message for failed pipeline
     *
     * @param projectName project name
     * @param pipelineName pipeline name
     * @return formatted message text
     */
    public String formatPipelineFailed(String projectName, String pipelineName) {
        return String.format("""
                        ❌ *Pipeline failed!*

                        Project: _%s_
                        Pipeline: *%s*
                        Please check the pipeline logs
                        """,
                projectName, pipelineName);
    }

    /**
     * Format message for deployed pipeline
     *
     * @param projectName project name
     * @param pipelineName pipeline name
     * @return formatted message text
     */
    public String formatPipelineDeployed(String projectName, String pipelineName) {
        return String.format("""
                        🚀 *Pipeline deployed successfully!*

                        Project: _%s_
                        Pipeline: *%s*
                        Deployment completed
                        """,
                projectName, pipelineName);
    }

    /**
     * Create inline-keyboard with MR link button
     *
     * @param url URL of merge request
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForMr(String url) {
        return buttonsForUrl("🔗 Open MR", url);
    }

    /**
     * Create inline-keyboard with Note link button
     *
     * @param url URL of note
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForNote(String url) {
        return buttonsForUrl("💬 Open comment", url);
    }

    /**
     * Create inline-keyboard with Pipeline link button
     *
     * @param url URL of Pipeline
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForPipeline(String url) {
        return buttonsForUrl("🚀 Open pipeline", url);
    }

    /**
     * Create inline-keyboard with Tag link button
     *
     * @param url URL of Tag
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForTag(String url) {
        return buttonsForUrl("🏷️ Open tag", url);
    }

    /**
     * Create inline-keyboard with Issue link button
     *
     * @param url URL of Issue
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForIssue(String url) {
        return buttonsForUrl("🐛 Open issue", url);
    }

    /**
     * Create inline-keyboard with link button
     *
     * @param url URL for link
     * @return inline-keyboard
     */
    public List<InlineKeyboardButtonRow> buttonsForUrl(String buttonText, String url) {
        if (url == null || url.isEmpty()) {
            return Collections.emptyList();
        }

        InlineKeyboardButtonRow.InlineKeyboardButton button =
                new InlineKeyboardButtonRow.InlineKeyboardButton(buttonText, url);

        InlineKeyboardButtonRow row = new InlineKeyboardButtonRow(Collections.singletonList(button));

        return Collections.singletonList(row);
    }
}
