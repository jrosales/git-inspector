package com.gitinspector.scheduling.codereviewstrategy;

import com.gitinspector.scheduling.PullRequestFetcher;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;

/**
 * This strategy will determine if a commit was correctly reviewed by checking
 * to see if the SHA associated with the commit on master has a matching
 * SHA on one of the commits on a pull request
 */
public class MatchingCommitShaStrategy implements ValidCodeReviewStrategy {

    @Override
    public boolean isCommitValid(GHCommit commit, PullRequestFetcher pullRequestFetcher) {
        for (GHPullRequest pullRequest : pullRequestFetcher.getCachedPullRequests()) {
            for (GHPullRequestCommitDetail prCommit : pullRequest.listCommits()) {
                //iterate over the pull requests
                //then iterate over the commits in the pull request
                //If the commit sha matches one of the pull request's commit shas, commit is valid
                if (prCommit.getSha().equals(commit.getSHA1())) {
                    return true;
                }
            }
        }

        return false;
    }
}
