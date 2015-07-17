package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.domain.recordable.Violation;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.scheduling.codereviewstrategy.MatchingCommitShaStrategy;
import com.gitinspector.scheduling.codereviewstrategy.MatchingJiraTicketStrategy;
import com.gitinspector.scheduling.codereviewstrategy.ValidCodeReviewStrategy;
import com.gitinspector.scheduling.codereviewstrategy.ValidCommitMessageStrategy;
import com.gitinspector.stats.GitStatisticsTracker;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;
import static com.gitinspector.stats.StatsLevel.REPOSITORY_LEVEL;

/**
 * The job identifies pull requests that have either been abandoned or
 * have already been merged
 */

public class ValidCodeReviewJob extends AbstractScheduledTask<Violation> {

    private static final Logger log = LoggerFactory.getLogger(ValidCodeReviewJob.class);

    private static final String COMMITS = "Commits";
    private static final String WITH_VALID_CODE_REVIEWS = "WithValidCodeReviews";

    private int numberOfDaysThreshold;

    public ValidCodeReviewJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
                              RepoOwnership repoOwnership, int numberOfDaysThreshold) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.numberOfDaysThreshold = numberOfDaysThreshold;
    }

    @Override
    public ReportResult<Violation, StringStatistic> execute() throws Exception {
        ReportResult<Violation, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("commitsWithValidCodeReviews");
        List<ValidCodeReviewStrategy> validCodeReviewStrategies = loadCodeReviewStrategies();

        for (GHRepository repository : targetRepositories.getTargetedRepositories()) {
            String repoFullName = repository.getFullName();

            final GHBranch masterBranch = repository.getBranches().get(repository.getMasterBranch());
            final GHCommit masterTipCommit = repository.getCommit(masterBranch.getSHA1());
            List<GHCommit> masterCommits = new ArrayList<>();
            assembleCommits(repository, masterTipCommit, masterCommits, numberOfDaysThreshold);

            PullRequestFetcher pullRequestFetcher = new PullRequestFetcher(repository, numberOfDaysThreshold);

            //iterate over the master commits we've assembled to verify
            //if the commit was valid
            for (GHCommit commit : masterCommits) {
                boolean isCommitValid = false;

                //iterate over the strategies we have in place for validating if our
                //commit is valid
                final Iterator<ValidCodeReviewStrategy> strategyIterator = validCodeReviewStrategies.iterator();
                while(strategyIterator.hasNext() && !isCommitValid) {
                    final ValidCodeReviewStrategy strategy = strategyIterator.next();
                    isCommitValid = strategy.isCommitValid(commit, pullRequestFetcher);
                }

                if (!isCommitValid) {
                    reportResult.addViolation(new Violation(getOrgNameFromRepoName(repoFullName),
                            repoFullName, getOwnerUsername(repoFullName)));
                }

                statsTracker.addHitToRepo(repoFullName, isCommitValid);
            }

            if (CollectionUtils.isNotEmpty(masterCommits)) {
                addStandardStatistics(reportResult, REPOSITORY_LEVEL, statsTracker, repoFullName, COMMITS, WITH_VALID_CODE_REVIEWS);
            }
        }

        // record the percentage of commits with valid review messages
        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, COMMITS, WITH_VALID_CODE_REVIEWS);
        }

        return reportResult;
    }

    /**
     * This method initializes our list of code review strategies that we have in place.
     * It loads them in the order that we would like them to be exercised.
     */
    private List<ValidCodeReviewStrategy> loadCodeReviewStrategies() {
        List<ValidCodeReviewStrategy> strategies = new ArrayList<>();
        strategies.add(new ValidCommitMessageStrategy());
        strategies.add(new MatchingCommitShaStrategy());
        strategies.add(new MatchingJiraTicketStrategy());
        return strategies;
    }

    @Override
    public String getRuleMessage() {
        return "Commit must have a \"reviewed by\" in the comment or must be associated with a Jira ticket.";
    }
}
