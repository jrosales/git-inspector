package com.gitinspector.scheduling.codereviewstrategy;

import com.gitinspector.scheduling.PullRequestFetcher;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This strategy will determine if a commit was correctly reviewed by
 * checking to see if the commit message has a Jira ticket number in it,
 * and then iterating over the list of pull requests to see if any of
 * those have the matching Jira ticket in their title.
 */
public class MatchingJiraTicketStrategy implements ValidCodeReviewStrategy {

    @Override
    public boolean isCommitValid(GHCommit commit, PullRequestFetcher pullRequestFetcher) {
        for (GHPullRequest pullRequest : pullRequestFetcher.getCachedPullRequests()) {
            //try to extract a Jira ticket number out of the commit message
            final String masterCommitJiraTicket = extractJiraTicketFromCommitMessage(commit.getCommitShortInfo().getMessage());
            if (masterCommitJiraTicket != null) {
                //Check to see if the title of the pull request contains the Jira ticket in the master commit message
                if (pullRequest.getTitle().toLowerCase().contains(masterCommitJiraTicket.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    private String extractJiraTicketFromCommitMessage(String commitMessage) {
        String jiraTicket = null;
        final Pattern pattern = Pattern.compile("(.*)([a-zA-Z]+\\-[0-9]+)(.*)", Pattern.DOTALL);
        final Matcher matcher = pattern.matcher(commitMessage);
        if (matcher.matches()) {
            jiraTicket = matcher.group(1);
        }

        return jiraTicket;
    }
}
