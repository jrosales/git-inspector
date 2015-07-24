package com.gitinspector.domain.recordable;

import com.gitinspector.domain.RecordableType;

/**
 * Represents a git rule violation.
 */
public class Violation extends ReportingRecordable {

    public Violation(String orgName, String repoFullName, String repoOwner) {
        super(RecordableType.VIOLATION, orgName, repoFullName, repoOwner);
    }

    @Override
    public String getStringVersion() {
        return this.toString();
    }

}
