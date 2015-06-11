package com.gitinspector.domain.recordable;

import com.gitinspector.domain.RecordableType;

/**
 * A basic object that can be recorded. Contains information about its type (violation or statistic), the repository, and repoOwner.
 */
public abstract class ReportingRecordable {

    private RecordableType type;
    private String repoFullName;
    private String orgName;
    private String repoOwner;

    public ReportingRecordable(RecordableType type, String orgName, String repoFullName, String repoOwner) {
        this.type = type;
        this.repoFullName = repoFullName;
        this.repoOwner = repoOwner;
        this.orgName = orgName;
    }

    public abstract String getStringVersion();

    public RecordableType getType() {
        return type;
    }

    public String getRepoFullName() {
        return repoFullName;
    }

    public String getOrgName() {
        return orgName;
    }

    public String getRepoOwner() {
        return repoOwner;
    }

    @Override
    public String toString() {
        return " type=" + type.getPrintableValue() +
               " repoFullName=" + repoFullName +
               " orgName=" + orgName +
               " repoOwner=" + repoOwner;
    }
}
