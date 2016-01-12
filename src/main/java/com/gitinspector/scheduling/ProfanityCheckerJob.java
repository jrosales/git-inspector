package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.BadCommit;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.domain.recordable.Violation;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.scheduling.AbstractScheduledTask;
import com.gitinspector.stats.GitStatisticsTracker;
import com.gitinspector.stats.StatsLevel;
import com.google.common.base.Splitter;
import org.apache.commons.collections4.CollectionUtils;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHContentSearchBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedSearchIterable;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProfanityCheckerJob extends AbstractScheduledTask<Violation> {

    private int numberOfDaysThreshold;

    private String PROFANITY_ENTITY = "Profanity";

    private String WITH_PROFANE_LANGUAGE_IN_SRC = "WithProfaneLanguageInSource";

    private String WITH_PROFANE_LANGUAGE_IN_COMMIT_MSG = "WithProfaneLanguageInCommitMsg";

    private String STATS_TRACKER_SRC_NAME = "reposWithProfanityInSrc";

    private String STATS_TRACKER_COMMIT_MSG_NAME = "reposWithProfanityInCommitMsg";

    private GitHub gitHub;

    private List<Pattern> searchPatterns;

    private List<String> profanityList;

    public ProfanityCheckerJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
                               RepoOwnership repoOwnership, int numberOfDaysThreshold, GitHub gitHub,
                               List<String> profanityList) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.numberOfDaysThreshold = numberOfDaysThreshold;
        this.gitHub = gitHub;
        this.profanityList = profanityList;
    }

    @Override
    public ReportResult<Violation, StringStatistic> execute() throws Exception {
        ReportResult<Violation, StringStatistic> reportResult = new ReportResult<>();

        for (GHRepository repository : targetRepositories.getTargetedRepositories()) {
            addOrgLevelStatsStats(reportResult, checkFiles(repository, reportResult),
                checkCommitMessages(repository, reportResult));
        }

        return reportResult;
    }

    @Override
    public String getRuleMessage() {
        return "Profanity Checker";
    }

    private void addOrgLevelStatsStats(ReportResult<Violation, StringStatistic> reportResult, GitStatisticsTracker srcTracker,
        GitStatisticsTracker commitMessageTracker) {

        for (String orgName : commitMessageTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, StatsLevel.ORG_LEVEL, commitMessageTracker, orgName, PROFANITY_ENTITY,
                WITH_PROFANE_LANGUAGE_IN_COMMIT_MSG);
        }

        for (String orgName : srcTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, StatsLevel.ORG_LEVEL, srcTracker, orgName, PROFANITY_ENTITY,
                WITH_PROFANE_LANGUAGE_IN_SRC);
        }
    }

    private GitStatisticsTracker checkCommitMessages(GHRepository repository, ReportResult<Violation, StringStatistic> reportResult)
        throws Exception {
        GitStatisticsTracker commitMessageTracker = new GitStatisticsTracker(STATS_TRACKER_COMMIT_MSG_NAME);
        boolean hasProfanity;
        String repoFullName = repository.getFullName();
        GHBranch masterBranch = repository.getBranches().get(repository.getMasterBranch());
        GHCommit masterTipCommit = repository.getCommit(masterBranch.getSHA1());
        List<GHCommit> masterCommits = new ArrayList<>();
        assembleCommits(repository, masterTipCommit, masterCommits, numberOfDaysThreshold);

        for (GHCommit commit : masterCommits) {
            hasProfanity = false;
            String commitMessage = commit.getCommitShortInfo().getMessage();

            for (String term : profanityList) {
                Pattern pattern = Pattern.compile(term);
                if (pattern.matcher(commitMessage).find()) {
                    reportResult.addViolation(
                        new BadCommit(getOrgNameFromRepoName(repoFullName), repoFullName, getOwnerUsername(repoFullName),
                            commit.getCommitShortInfo().getCommitter().getName(), commit.getSHA1()));
                    hasProfanity = true;
                    break;
                }
            }

            commitMessageTracker.addHitToRepo(repoFullName, hasProfanity);
        }

        if (CollectionUtils.isNotEmpty(masterCommits)) {
            addStandardStatistics(reportResult, StatsLevel.REPOSITORY_LEVEL, commitMessageTracker, repoFullName, PROFANITY_ENTITY,
                WITH_PROFANE_LANGUAGE_IN_COMMIT_MSG);
        }

        return commitMessageTracker;
    }

    private GitStatisticsTracker checkFiles(GHRepository repository, ReportResult<Violation, StringStatistic> reportResult) {
        GitStatisticsTracker srcTracker = new GitStatisticsTracker(STATS_TRACKER_SRC_NAME);
        String repoFullName = repository.getFullName();

        for (String term : profanityList) {
            GHContentSearchBuilder search = gitHub.searchContent();
            search.repo(repository.getFullName()).q("\"" + term + "\"");
            PagedSearchIterable<GHContent> list = search.list();

            if (list.getTotalCount() > 0) {
                reportResult.addViolation(
                    new Violation(getOrgNameFromRepoName(repoFullName), repoFullName, getOwnerUsername(repoFullName)));
            }

            srcTracker.addHitToRepo(repoFullName, list.getTotalCount() > 0);
        }

        addStandardStatistics(reportResult, StatsLevel.REPOSITORY_LEVEL, srcTracker, repoFullName, PROFANITY_ENTITY,
            WITH_PROFANE_LANGUAGE_IN_SRC);

        return srcTracker;
    }
}
