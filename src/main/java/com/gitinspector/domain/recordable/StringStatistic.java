package com.gitinspector.domain.recordable;

import com.gitinspector.domain.RecordableType;

/**
 * A simple statistic that maintains a key value pair as Strings.
 */
public class StringStatistic extends ReportingRecordable {

    private String key;
    private String value;

    public StringStatistic(String orgName, String repoFullName, String owner, String key, String value) {
        super(RecordableType.STATISTIC, orgName, repoFullName, owner);
        this.key = key;
        this.value = value;
    }

    @Override
    public String getStringVersion() {
        return this.toString();
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return super.toString() +
               " " + key + "=" + value;
    }
}
