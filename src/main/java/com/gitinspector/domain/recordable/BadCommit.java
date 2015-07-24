package com.gitinspector.domain.recordable;

/**
 * Represents a commit that violates a Git Inspector rule.
 */
public class BadCommit extends Violation {

    private String committer;

    private String commitSha;

    private String commitUrl;

    public BadCommit(String orgName, String repoFullName, String repoOwner, String committer, String commitSha) {
        super(orgName, repoFullName, repoOwner);
        this.committer = committer;
        this.commitSha = commitSha;
        this.commitUrl = "/" + repoFullName + "/commit/" + commitSha;
    }

    public String getCommitter() {
        return committer;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public String getCommitUrl() {
        return commitUrl;
    }

    @Override
    public String toString() {
        return super.toString() +
            " committer=" + committer +
            " commitSHA=" + commitSha +
            " commitURL=" + commitUrl;
    }
}
