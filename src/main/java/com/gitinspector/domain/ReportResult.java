package com.gitinspector.domain;

import com.gitinspector.domain.recordable.ReportingRecordable;

import java.util.ArrayList;
import java.util.List;

/**
 * The results of running a git report. Tracks violations and statistics.
 */
public class ReportResult<V extends ReportingRecordable, S extends ReportingRecordable> {
    private List<V> violations = new ArrayList<>();
    private List<S> statistics = new ArrayList<>();
    private List<ReportingRecordable> allReportingRecordables = new ArrayList<>();

    public ReportResult() {
    }

    public void addViolation(V violation) {
        violations.add(violation);
        allReportingRecordables.add(violation);
    }

    public void addStatistic(S statistic) {
        statistics.add(statistic);
        allReportingRecordables.add(statistic);
    }

    public List<V> getViolations() {
        return violations;
    }

    public List<S> getStatistics() {
        return statistics;
    }

    public List<ReportingRecordable> getAllReportingRecordables() {
        return allReportingRecordables;
    }
}
