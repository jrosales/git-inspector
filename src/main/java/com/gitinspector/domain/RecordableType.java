package com.gitinspector.domain;

/**
 * A enum to represent the various types of entities that we can record.
 */
public enum RecordableType {
    STATISTIC("statistic"), VIOLATION("violation");

    private String printableValue;

    RecordableType(String printableValue) {
        this.printableValue = printableValue;
    }

    public String getPrintableValue() {
        return printableValue;
    }
}
