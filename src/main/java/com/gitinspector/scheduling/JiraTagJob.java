package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.BadCommit;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.domain.recordable.Violation;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import org.kohsuke.github.GHBranch;
import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHRepository;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;
import static com.gitinspector.stats.StatsLevel.REPOSITORY_LEVEL;

/**
 * Identifies commits with no JIRA tag.
 */
@ManagedResource(description = "Enables JMX management of the JIRA Tag job")
public class JiraTagJob extends AbstractScheduledTask<Violation> {

    private static final String JIRA_TAGS = "JiraTags";

    private static final String WITH_VALID_JIRA_TAGS = "WithValidJiraTag";

    private int numberOfDaysThreshold;

    public JiraTagJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
        RepoOwnership repoOwnership, int numberOfDaysThreshold) {
        super(messageRecorder, repoOwnership, targetRepositories);
        this.numberOfDaysThreshold = numberOfDaysThreshold;
    }

    @Override
    public ReportResult<Violation, StringStatistic> execute() throws Exception {

        ReportResult<Violation, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("commitsWithJIRATag");

        for (GHRepository repo : targetRepositories.getTargetedRepositories()) {
            String repoFullName = repo.getFullName();

            final GHBranch masterBranch = repo.getBranches().get(repo.getMasterBranch());
            final GHCommit masterTipCommit = repo.getCommit(masterBranch.getSHA1());
            List<GHCommit> masterCommits = new ArrayList<>();
            assembleCommits(repo, masterTipCommit, masterCommits, numberOfDaysThreshold);

            for (GHCommit commit : masterCommits) {
                String commitMessage = commit.getCommitShortInfo().getMessage();

                boolean isCommitValid = isCommitValid(commitMessage);

                if (!isCommitValid) {
                    reportResult.addViolation(new BadCommit(getOrgNameFromRepoName(repoFullName), repoFullName,
                        getOwnerUsername(repoFullName), commit.getCommitShortInfo().getCommitter().getName(), commit.getSHA1()));
                }

                statsTracker.addHitToRepo(repoFullName, isCommitValid);
            }

            addStandardStatistics(reportResult, REPOSITORY_LEVEL, statsTracker, repoFullName, JIRA_TAGS, WITH_VALID_JIRA_TAGS);
        }

        // record the percentage of commits with valid JIRA tag for each organization that we encountered
        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, JIRA_TAGS, WITH_VALID_JIRA_TAGS);
        }

        return reportResult;
    }

    @ManagedAttribute
    public int getNumberOfDaysThreshold() {
        return this.numberOfDaysThreshold;
    }

    @ManagedAttribute
    public void setNumberOfDaysThreshold(int numberOfDaysThreshold) {
        this.numberOfDaysThreshold = numberOfDaysThreshold;
    }

    @Override
    public String getRuleMessage() {
        return "Commit should begin with an associated JIRA tag.";
    }

    protected boolean isCommitValid(String commitMessage) {
        Pattern pattern = Pattern.compile(".*[A-Z]+\\-[0-9]+.*|^\\[(maven|grunt)-release-plugin\\].*", Pattern.DOTALL);
        return pattern.matcher(commitMessage).matches();
    }
}
