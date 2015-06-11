package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.StaleObject;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import org.joda.time.DateTime;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.PagedIterator;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;

/**
 * Identifies repositories that have not been committed to in a long time
 */
@ManagedResource(description = "Enables JMX management of the stale repositories job")
public class StaleRepositoriesJob extends AbstractScheduledTask<StaleObject> {
    private int daysSinceLastCommit;

    public StaleRepositoriesJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
                                RepoOwnership repoOwnership, int daysSinceLastCommit) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.daysSinceLastCommit = daysSinceLastCommit;
    }

    @Override
    public ReportResult<StaleObject, StringStatistic> execute() throws Exception {
        ReportResult<StaleObject, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("reposWithRecentCommits");

        for (GHRepository repo : targetRepositories.getTargetedRepositories()) {
            boolean repoIsStale = false;
            final PagedIterator<GHCommit> iterator = repo.listCommits().iterator();
            final String repoFullName = repo.getFullName();
            if (iterator.hasNext()) {
                final GHCommit lastCommit = iterator.next();
                final GHCommit.ShortInfo commitShortInfo = lastCommit.getCommitShortInfo();
                DateTime commitDate = new DateTime(commitShortInfo.getCommitter().getDate());
                repoIsStale = commitDate.isBefore(DateTime.now().minusDays(daysSinceLastCommit));
                if (repoIsStale) {
                    reportResult.addViolation(new StaleObject(getOrgNameFromRepoName(repoFullName),
                                                              repoFullName,
                                                              getOwnerUsername(repoFullName),
                                                              repoFullName,
                                                              commitShortInfo.getCommitter().getEmail(),
                                                              commitDate.toString(LAST_TOUCH_DATE_FORMAT)));
                }
            }
            statsTracker.addHitToRepo(repoFullName, !repoIsStale);
        }

        // record the percentage of repos with recent commits for each organization that we encountered
        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, "Repos", "WithRecentCommits");
        }

        return reportResult;
    }

    @Override
    public String getRuleMessage() {
        return "Repository should have a commit to master within the last " + daysSinceLastCommit + " days.";
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
