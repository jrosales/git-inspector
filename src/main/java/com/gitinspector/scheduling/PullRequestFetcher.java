package com.gitinspector.scheduling;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterator;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a helper class that will lazy load pull requests
 * associated with a specified repository
 */
public class PullRequestFetcher {

    private List<GHPullRequest> cachedPullRequests;

    private GHRepository repository;

    private int numberOfDaysThreshold;

    public PullRequestFetcher(GHRepository repository, int numberOfDaysThreshold) {
        this.repository = repository;
        this.numberOfDaysThreshold = numberOfDaysThreshold;
    }

    public List<GHPullRequest> getCachedPullRequests() {
        if (cachedPullRequests == null) {
            cachedPullRequests = new ArrayList<>();
            LocalDate now = LocalDate.now();
            final PagedIterator<GHPullRequest> prIterator = repository.listPullRequests(GHIssueState.CLOSED).iterator();
            while (prIterator.hasNext()) {
                final GHPullRequest pullRequest = prIterator.next();
                //We need to keep an eye on the date range for the pull requests we're pulling back
                //and if need be, modify the threshold value for that.
                if (Days.daysBetween(LocalDate.fromDateFields(pullRequest.getClosedAt()), now).getDays() < numberOfDaysThreshold) {
                    cachedPullRequests.add(pullRequest);
                }
            }
        }
        return this.cachedPullRequests;
    }
}
