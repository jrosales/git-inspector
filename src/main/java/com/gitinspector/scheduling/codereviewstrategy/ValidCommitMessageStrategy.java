package com.gitinspector.scheduling.codereviewstrategy;

import com.gitinspector.scheduling.PullRequestFetcher;
import org.kohsuke.github.GHCommit;

import java.util.regex.Pattern;

/**
 * This strategy will determine if a commit has was correctly reviewed by checking
 * to see if there is a "reviewed by" stated in the commit message
 */
public class ValidCommitMessageStrategy implements ValidCodeReviewStrategy {


    @Override
    public boolean isCommitValid(GHCommit commit, PullRequestFetcher pullRequestFetcher) {
        final String commitMessage = commit.getCommitShortInfo().getMessage();

        //If the commit message contains a "reviewed by" message, commit is valid
        return doesCommitContainReviewedBy(commitMessage);
    }

    private boolean doesCommitContainReviewedBy(String commitMessage) {
        final Pattern pattern = Pattern.compile(".*reviewed by.*", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(commitMessage).matches();
    }
}
