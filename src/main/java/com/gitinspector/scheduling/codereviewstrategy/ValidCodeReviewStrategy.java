package com.gitinspector.scheduling.codereviewstrategy;

import com.gitinspector.scheduling.PullRequestFetcher;
import org.kohsuke.github.GHCommit;

/**
 * Common interface representing the strategies that will be used
 * to deterimine if a a commit is associated with a valid review.
 */
public interface ValidCodeReviewStrategy {

    boolean isCommitValid(GHCommit commit, PullRequestFetcher pullRequestFetcher);
}
