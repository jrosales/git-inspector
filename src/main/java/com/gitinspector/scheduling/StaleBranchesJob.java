package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.StaleObject;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import org.joda.time.DateTime;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.Map;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;
import static com.gitinspector.stats.StatsLevel.REPOSITORY_LEVEL;

/**
 * Identifies branches that have not been committed to in a long time
 */
@ManagedResource(description = "Enables JMX management of the stale branches job")
public class StaleBranchesJob extends AbstractScheduledTask<StaleObject> {

    private static final String BRANCHES = "Branches";
    private static final String WITH_RECENT_COMMITS = "WithRecentCommits";

    private int daysSinceLastCommit;

    public StaleBranchesJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
                            RepoOwnership repoOwnership, int daysSinceLastCommit) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.daysSinceLastCommit = daysSinceLastCommit;
    }

    @Override
    public ReportResult<StaleObject, StringStatistic> execute() throws Exception {
        ReportResult<StaleObject, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("branchesWithRecentCommits");

        for (GHRepository repo : targetRepositories.getTargetedRepositories()) {
            final Map<String, GHBranch> branches = repo.getBranches();

            // ignore repos that only have a master branch and nothing else
            if (branches.size() <= 1) {
                continue;
            }

            String repoFullName = repo.getFullName();
            for (GHBranch branch : branches.values()) {
                // master doesn't count
                if ("master".equalsIgnoreCase(branch.getName())) {
                    continue;
                }

                final GHCommit commit = branch.getOwner().getCommit(branch.getSHA1());
                final GHCommit.ShortInfo commitShortInfo = commit.getCommitShortInfo();
                DateTime commitDate = new DateTime(commitShortInfo.getCommitter().getDate());
                boolean isBranchStale = commitDate.isBefore(DateTime.now().minusDays(daysSinceLastCommit));
                if (isBranchStale) {
                    reportResult.addViolation(new StaleObject(getOrgNameFromRepoName(repoFullName),
                                                              repoFullName,
                                                              getOwnerUsername(repoFullName),
                                                              branch.getName(),
                                                              commitShortInfo.getCommitter().getEmail(),
                                                              commitDate.toString(LAST_TOUCH_DATE_FORMAT)));
                }

                statsTracker.addHitToRepo(repoFullName, !isBranchStale);
            }

            addStandardStatistics(reportResult, REPOSITORY_LEVEL, statsTracker, repoFullName, BRANCHES, WITH_RECENT_COMMITS);
        }

        // record the percentage of branches with recent commits for each organization that we encountered
        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, BRANCHES, WITH_RECENT_COMMITS);
        }

        return reportResult;
    }

    @Override
    public String getRuleMessage() {
        return "Branch should have a commit within the last " + daysSinceLastCommit + " days.";
    }

    @ManagedAttribute
    public int getDaysSinceLastCommit() {
        return daysSinceLastCommit;
    }

    @ManagedAttribute
    public void setDaysSinceLastCommit(int daysSinceLastCommit) {
        this.daysSinceLastCommit = daysSinceLastCommit;
    }
}
