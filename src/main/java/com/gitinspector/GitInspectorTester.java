package com.gitinspector;

import org.kohsuke.github.GHCommit;
import org.kohsuke.github.GHIssueComment;
import org.kohsuke.github.GHIssueState;
import org.kohsuke.github.GHPullRequest;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.github.PagedIterable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * NOTE: This is a temporary class for testing of the Kohsuke API.
 */
public class GitInspectorTester {

    public static final List<String> TEAM_GIT_ORG_NAMES = Arrays.asList("OMDev",
                                                                        "admin-tools",
                                                                        "payment-services");
    public static final List<String> TEAM_NON_ORG_REPOS = Arrays.asList("core-api/api-orders",
                                                                        "core-api/api-endpoint-dao");

    private GitHub gitHub;

    public static void main(String args[]) throws IOException, InterruptedException {
        // for manual testing only if you don't want to do mvn jetty:run and let the scheduled tasks run
        GitInspectorTester gitInspector = new GitInspectorTester("http://github.wvrgroup.internal/api/v3/", "replace me with your token");

        // get all repositories associated with our team and print them out
        final List<GHRepository> teamRepos = gitInspector.getTeamRepos();
        for (GHRepository repo : teamRepos) {
//            System.out.println(repo.getFullName());
        }
        System.out.println();

        // get a particular repo for testing
        final GHRepository omapiRepo = gitInspector.getGitHub().getRepository("OMDev/omapi");

//        gitInspector.reportOnStaleBranches(omapiRepo, 21);

//        gitInspector.reportOnReadmeFiles(omapiRepo);

        // display information about the last few master commits from a repo
//        gitInspector.displayCommits(omapiRepo, 5);

        gitInspector.displayInfoOnPRs(omapiRepo);

    }

    public GitInspectorTester(String gitHubServer, String gitHubToken) throws IOException {
        gitHub = GitHub.connectToEnterprise(gitHubServer, gitHubToken);
    }

    public GitHub getGitHub() {
        return gitHub;
    }

    public List<GHRepository> getTeamRepos() throws IOException {
        List<GHRepository> teamRepos = new ArrayList<>();

        // get all of the git repos for the orgs this team owns
        for (String gitOrgName : TEAM_GIT_ORG_NAMES) {
            teamRepos.addAll(gitHub.getOrganization(gitOrgName).getRepositories().values());
        }

        // get all of the git repos that the team owns but that are not a part of their git orgs
        for (String nonOrgRepoName : TEAM_NON_ORG_REPOS) {
            GHRepository repository = gitHub.getRepository(nonOrgRepoName);
            teamRepos.add(repository);
        }

        return teamRepos;
    }

    public void displayCommits(GHRepository repo, int numCommits) {
        System.out.println("Commits for repo: " + repo.getFullName());
        int count = 1;
        for (GHCommit commit : repo.listCommits()) {
            System.out.println("  Commit: " + commit.getSHA1());
            System.out.println("    Date: " + commit.getCommitShortInfo().getCommitter().getDate());
            System.out.printf("    Message: " + commit.getCommitShortInfo().getMessage());
            System.out.println();
            System.out.println();
            if (count++ >= numCommits) {
                break;

            }
        }
    }

    public void displayInfoOnPRs(GHRepository repo) throws IOException {
        final PagedIterable<GHPullRequest> closedPRIterable = repo.listPullRequests(GHIssueState.CLOSED);

        List<GHPullRequest> closedPRs = closedPRIterable.asList();
        Collections.sort(closedPRs, new Comparator<GHPullRequest>() {
            @Override
            public int compare(GHPullRequest o1, GHPullRequest o2) {
                return o2.getClosedAt().compareTo(o1.getClosedAt());
            }
        });

        for (int i = 0; i < 3; i++) {
            GHPullRequest closedPR = closedPRs.get(i);
            System.out.println("PR " + closedPR.getTitle());
            System.out.println("  Closed: " + closedPR.getClosedAt());
            final List<GHIssueComment> comments = closedPR.getComments();
            if (!comments.isEmpty()) {
                System.out.println("  Last commenter: " + comments.get(comments.size() - 1).getUser().getEmail());
                System.out.println("  Last comment");
                System.out.println("  ------------");
                System.out.println(comments.get(comments.size() - 1).getBody());
                System.out.println("  ------------");
            }
            System.out.println();
        }
    }

}
