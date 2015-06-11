/*
 * Copyright Homeaway, Inc 2015-Present. All Rights Reserved.
 * No unauthorized use of this software.
 */
package com.gitinspector.domain.recordable;

/**
 * Represents a violation due to a stale object.
 */
public class StaleObject extends Violation {

    private String staleObjectName;
    private String lastCommitter;
    private String formattedLastCommitDate;

    public StaleObject(String orgName, String repoFullName, String owner, String staleObjectName, String lastCommitter,
                       String formattedLastCommitDate) {
        super(orgName, repoFullName, owner);
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
