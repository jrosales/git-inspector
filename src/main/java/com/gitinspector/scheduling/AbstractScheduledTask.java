package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.GitUser;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.ReportingRecordable;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import com.gitinspector.stats.StatsLevel;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.joda.time.DateTime;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;

import java.io.IOException;
import java.util.List;

/**
 * Abstract class for all git related scheduled tasks.
 * Deals with things like logging of start/stop messages, error handling, and ensuring that a rule's
 * description gets logged before execution begins.
 */
public abstract class AbstractScheduledTask<V extends ReportingRecordable> implements Runnable {

    protected static final String LAST_TOUCH_DATE_FORMAT = "yyyy-MM-dd";

    private String taskName;

    protected TaskMessageRecorder messageRecorder;

    protected RepoOwnership repoOwnership;

    protected TargetRepositories targetRepositories;

    public AbstractScheduledTask(TaskMessageRecorder messageRecorder, RepoOwnership repoOwnership,
        TargetRepositories targetRepositories) {
        taskName = this.getClass().getSimpleName();
        this.messageRecorder = messageRecorder;
        this.repoOwnership = repoOwnership;
        this.targetRepositories = targetRepositories;
    }

    /**
     * Invoked when the task is executed. Abstracts some of the common logic for tasks like recording
     * a start/stop message and dealing with error handling. Will invoke the execute() method which all
     * subclasses must implement.
     */
    public void run() {
        messageRecorder.recordStateRelatedMessage(taskName, "starting");
        messageRecorder.recordReportingMessage(taskName, "message=" + getRuleMessage());

        try {
            final ReportResult<V, StringStatistic> reportResult = execute();

            for (ReportingRecordable reportingRecordable : reportResult.getAllReportingRecordables()) {
                messageRecorder.recordReportingRecordable(taskName, reportingRecordable);
            }
        } catch (Exception e) {
            messageRecorder.recordError(taskName, ExceptionUtils.getFullStackTrace(e));
        }

        messageRecorder.recordStateRelatedMessage(taskName, "complete");
    }

    /**
     * To be overriden by base classes. Exceptions should be allowed to bubble up. They will be handled.
     */
    public abstract ReportResult<V, StringStatistic> execute() throws Exception;

    /**
     * @return a message describing the details of the rule. This will be recorded with the task begins execution.
     */
    public abstract String getRuleMessage();

    /**
     * Adds the standard 3 statistics we typically record to the provided ReportResult.
     * 1) The total number of object evaluated
     * 2) The total number that evaluated positively
     * 3) The percentage that evaluated positively
     *
     * @param reportResult       a ReportResult to which the statistics should be added
     * @param statsLevel         the level to log the stats at (repo or org)
     * @param statsTracker       the StatsTracker from which the stats values can be retrieved
     * @param statsTargetEntity  the full name of the repository (if statsLevel is repository) or the name of the org
     *                           (if statsLevel is org)
     * @param objectBeingCounted the name of the object being counted;
     *                           will be used as a part of the stat keys (e.g. branches, repos, etc)
     * @param positiveStatement  a positive statement to be appended to the stat key (e.g. WithRecentCommits, WithValidReadMe)
     */
    protected void addStandardStatistics(ReportResult<V, StringStatistic> reportResult, StatsLevel statsLevel,
        GitStatisticsTracker statsTracker, String statsTargetEntity, String objectBeingCounted, String positiveStatement) {

        // statsTargetEntity will either be an org name (e.g. OMDev) or a repo full name (e.g. OMDev/omapi)

        // for org level stats, statsTargetEntity will be the org name; otherwise we need to parse the org name from the repo full name
        String orgName = StatsLevel.ORG_LEVEL.equals(statsLevel) ? statsTargetEntity : StringUtils.substringBefore(statsTargetEntity, "/");

        // for org level stats we do not have a repo name, just use n/a; otherwise use the provided target entity as the repo name
        String repoFullName = StatsLevel.ORG_LEVEL.equals(statsLevel) ? "n/a" : statsTargetEntity;

        // get the owner (only relevant for repo-level stats)
        String ownerUsername = StatsLevel.REPOSITORY_LEVEL.equals(statsLevel) ? getOwnerUsername(statsTargetEntity) : "n/a";

        // for org level stats we post-pend the statistic name with the word "org"
        String statNameSuffix = StatsLevel.ORG_LEVEL.equals(statsLevel) ? "Org" : "";

        // log a stat called "numberOf<objectname>" with a value containing the total number of hits
        final String totalHits = String.valueOf(statsTracker.getTotalHits(statsLevel, statsTargetEntity));
        reportResult.addStatistic(new StringStatistic(orgName, repoFullName, ownerUsername,
            "numberOf" + objectBeingCounted + statNameSuffix, totalHits));

        // log a stat called "numberOf<objectname><positiveStatement" with a value containing the # of positive hits
        final String positiveHits = String.valueOf(statsTracker.getPositiveHits(statsLevel, statsTargetEntity));
        reportResult.addStatistic(new StringStatistic(orgName, repoFullName, ownerUsername,
            "numberOf" + objectBeingCounted + positiveStatement + statNameSuffix, positiveHits));

        // log a stat called "percentOf<objectname><positiveStatement" with a value containing the % of positive hits
        final String percentPositiveHits = String.valueOf(statsTracker.getPercentageOfPositiveHits(statsLevel, statsTargetEntity));
        reportResult.addStatistic(new StringStatistic(orgName, repoFullName, ownerUsername,
            "percentOf" + objectBeingCounted + positiveStatement + statNameSuffix, percentPositiveHits));
    }

    /**
     * Get the username of the owner for the provided repository.
     *
     * @param repoFullName the fullname of the repo whose owner we are to look up (e.g. OMDev/omapi)
     * @return the username of the repository owner or a) "n/a" if repoFullName is null or
     * b) "unknown" if the owner could not be determined
     */
    protected String getOwnerUsername(String repoFullName) {
        if (repoFullName == null) {
            return "n/a";
        }
        final GitUser repositoryOwner = repoOwnership.getRepositoryOwner(repoFullName);
        return repositoryOwner == null ? "unknown" : repositoryOwner.getUsername();
    }

    protected String getOrgNameFromRepoName(String repoFullName) {
        return StringUtils.substringBefore(repoFullName, "/");
    }

    //TODO: Move this to a utility class
    protected void assembleCommits(GHRepository repo, GHCommit currentCommit, List<GHCommit> assembledCommits, int numberOfDaysThreshold)
            throws IOException {
        // NOTE: "parent" commits are predecessor commits (i.e. the commits that lead up to the current commit)

        // check if the current commit is within our time threshold and if not, don't bother adding it or continuing
        DateTime commitDate = new DateTime(currentCommit.getCommitShortInfo().getCommitter().getDate());
        if (commitDate.isBefore(DateTime.now().minusDays(numberOfDaysThreshold))) {
            return;
        }

        // go ahead and add the current commit
        assembledCommits.add(currentCommit);

        // check if this commit has any parents and if not, end our recursion
        final List<String> parentSHA1s = currentCommit.getParentSHA1s();
        if(parentSHA1s == null || parentSHA1s.isEmpty()) {
            return;
        }

        // recurse over all of the parents
        for(String parentSHA1 : parentSHA1s) {
            assembleCommits(repo, repo.getCommit(parentSHA1), assembledCommits, numberOfDaysThreshold);
        }
    }
}
