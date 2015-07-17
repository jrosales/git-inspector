package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.StaleObject;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHPullRequestCommitDetail;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;
import static com.gitinspector.stats.StatsLevel.REPOSITORY_LEVEL;

/**
 * The job identifies pull requests that have either been abandoned or
 * have already been merged
 */
@ManagedResource(description = "Enables JMX management of the stale pull requests job")
public class StalePullRequestsJob extends AbstractScheduledTask<StaleObject> {

    private static final Logger log = LoggerFactory.getLogger(StalePullRequestsJob.class);

    private static final String PULL_REQUESTS = "PullRequests";

    private static final String WITH_RECENT_COMMITS = "WithRecentCommits";

    private int daysSinceLastCommit;

    public StalePullRequestsJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
        RepoOwnership repoOwnership, int daysSinceLastCommit) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.daysSinceLastCommit = daysSinceLastCommit;
    }

    @Override
    public ReportResult<StaleObject, StringStatistic> execute() throws Exception {
        ReportResult<StaleObject, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("pullRequestsWithRecentCommits");

        final LocalDate now = LocalDate.now();

        for (GHRepository repository : targetRepositories.getTargetedRepositories()) {
            final String repoFullName = repository.getFullName();
            //Retrieve all open pull requests
            final List<GHPullRequest> pullRequests = repository.getPullRequests(GHIssueState.OPEN);
            for (GHPullRequest pullRequest : pullRequests) {
                String stalePullRequestName = repoFullName + "/pull/" + Integer.toString(pullRequest.getNumber());
                final GHCommit commit = repository.getCommit(pullRequest.getHead().getSha());
                final LocalDate lastCommitDate = LocalDate.fromDateFields(commit.getCommitShortInfo().getCommitter().getDate());

                final boolean isPullRequestStale = Days.daysBetween(lastCommitDate, now).getDays() > daysSinceLastCommit;
                if (isPullRequestStale) {
                    reportResult.addViolation(
                            new StaleObject(
                                    getOrgNameFromRepoName(repoFullName),
                                    repoFullName,
                                    getOwnerUsername(repoFullName),
                                    stalePullRequestName,
                                    getLastTouchedBy(pullRequest),
                                    lastCommitDate.toString(LAST_TOUCH_DATE_FORMAT)
                            )
                    );
                }

                statsTracker.addHitToRepo(repoFullName, !isPullRequestStale);
            }

            if (CollectionUtils.isNotEmpty(pullRequests)) {
                addStandardStatistics(reportResult, REPOSITORY_LEVEL, statsTracker, repoFullName, PULL_REQUESTS, WITH_RECENT_COMMITS);
            }
        }

        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, PULL_REQUESTS, WITH_RECENT_COMMITS);
        }

        return reportResult;
    }

    @Override
    public String getRuleMessage() {
        return String.format("Pull request must have activity on it within the last %s days.", daysSinceLastCommit);
    }

    @ManagedAttribute
    public int getDaysSinceLastCommit() {
        return daysSinceLastCommit;
    }

    @ManagedAttribute
    public void setDaysSinceLastCommit(int daysSinceLastCommit) {
        this.daysSinceLastCommit = daysSinceLastCommit;
    }

    /**
     * Extracts the user associated with the last activity made on a pull request
     *
     * @param pullRequest Pull request in question
     * @return Name of the user associated with the last activity on the pull request
     * @throws IOException
     */
    private String getLastTouchedBy(GHPullRequest pullRequest) throws IOException {
        //grab the most recent commit and return the author
        String lastCommitOwner = "unavailable";
        if (pullRequest.listCommits().iterator().hasNext()) {
            final GHPullRequestCommitDetail mostRecentCommit = pullRequest.listCommits().iterator().next();
            lastCommitOwner = mostRecentCommit.getCommit().getCommitter().getName();
        }
        return lastCommitOwner;
    }
}
