package com.gitinspector.domain.recordable;

/**
 * Represents a violation due to a stale object.
 */
public class StaleObject extends Violation {

    private String staleObjectName;

    private String lastCommitter;

    private String formattedLastCommitDate;

    public StaleObject(String orgName, String repoFullName, String repoOwner, String staleObjectName, String lastCommitter,
        String formattedLastCommitDate) {
        super(orgName, repoFullName, repoOwner);
        this.staleObjectName = staleObjectName;
        this.lastCommitter = lastCommitter;
        this.formattedLastCommitDate = formattedLastCommitDate;
    }

    public String getStaleObjectName() {
        return staleObjectName;
    }

    public String getLastCommitter() {
        return lastCommitter;
    }

    public String getFormattedLastCommitDate() {
        return formattedLastCommitDate;
    }

    @Override
    public String toString() {
        return super.toString() +
            " staleObjectName=" + staleObjectName +
            " lastCommitter=" + lastCommitter +
            " formattedLastCommitDate=" + formattedLastCommitDate;
    }
}
