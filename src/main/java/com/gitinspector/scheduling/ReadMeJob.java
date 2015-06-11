package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.domain.ReportResult;
import com.gitinspector.domain.recordable.StringStatistic;
import com.gitinspector.domain.recordable.Violation;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import com.gitinspector.stats.GitStatisticsTracker;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GHRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import static com.gitinspector.stats.StatsLevel.ORG_LEVEL;

/**
 * Checks that git repositories have a valid README.md file
 */
public class ReadMeJob extends AbstractScheduledTask<Violation> {

    private static final Logger log = LoggerFactory.getLogger(ReadMeJob.class);

    public ReadMeJob(TargetRepositories targetRepositories, TaskMessageRecorder messageRecorder,
                     RepoOwnership repoOwnership) {
        super(messageRecorder, repoOwnership, targetRepositories);
    }

    @Override
    public ReportResult<Violation, StringStatistic> execute() throws Exception {
        ReportResult<Violation, StringStatistic> reportResult = new ReportResult<>();
        GitStatisticsTracker statsTracker = new GitStatisticsTracker("reposWithReadMe");

        for (GHRepository repo : targetRepositories.getTargetedRepositories()) {
            final String repoFullName = repo.getFullName();
            final boolean foundValidReadMe = isReadMeValid(getReadMeContents(repo), repoFullName);
            if (!foundValidReadMe) {
                reportResult.addViolation(new Violation(getOrgNameFromRepoName(repoFullName),
                                                        repoFullName,
                                                        getOwnerUsername(repoFullName)));
            }
            statsTracker.addHitToRepo(repoFullName, foundValidReadMe);
        }

        // record the percentage of repos with read me files for each organization that we encountered
        for (String orgName : statsTracker.getAllOrgsWithHits()) {
            addStandardStatistics(reportResult, ORG_LEVEL, statsTracker, orgName, "Repos", "WithValidReadMe");
        }

        return reportResult;
    }

    @Override
    public String getRuleMessage() {
        return "Repository must have a README.md file meeting the following requirements: "
               + "a) more than 1 line "
               + " b) contain a valid Ownership section";
    }

    /**
     * Returns whether or not the provided readme contents meet our standards.
     *
     * @param readMeContents a String representing the contents of the readme file
     * @param repoFullName   the full name of the repository from which the read me cam (e.g. OMDev/omapi)
     * @return true if the readme meets our standards; false otherwise
     */
    private boolean isReadMeValid(String readMeContents, String repoFullName) {
        return (readMeContents != null
                && StringUtils.countMatches(readMeContents, "\n") > 1
                && repoOwnership.getRepositoryOwner(repoFullName) != null);
    }

    /**
     * Return the contents of the repository's README.md file or null if one was not found.
     *
     * @param repo the repository whose README.md is to be returned
     * @return the contents of this repo's README.md file or null if none could be found
     * @throws IOException if there was an error reading the repository file information
     */
    private String getReadMeContents(GHRepository repo) throws IOException {
        try {
            final List<GHContent> directoryContent = repo.getDirectoryContent("");
            for (GHContent content : directoryContent) {
                if ("README.MD".equalsIgnoreCase(content.getName())) {
                    return content.getContent();
                }
            }
        } catch (FileNotFoundException e) {
            log.warn("Cannot retrieve contents of empty repository {}.", repo.getName());
        }
        return null;
    }

}
