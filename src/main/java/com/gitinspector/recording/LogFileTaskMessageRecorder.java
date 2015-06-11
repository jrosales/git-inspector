package com.gitinspector.recording;

import com.gitinspector.domain.recordable.ReportingRecordable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TaskMessageRecorder that writes to log files.
 */
public class LogFileTaskMessageRecorder implements TaskMessageRecorder {
    private static final Logger reportingLog = LoggerFactory.getLogger("com.gitinspector.ReportingLogger");
    private static final Logger log = LoggerFactory.getLogger(LogFileTaskMessageRecorder.class);

    @Override
    public void recordStateRelatedMessage(String taskName, String msg) {
        log.info(taskName + " : " + msg);
    }

    @Override
    public void recordError(String taskName, String msg) {
        log.error(taskName + " : " + msg);
    }

    @Override
    public void recordReportingMessage(String taskName, String msg) {
        String newMsg = taskName + " : " + msg;
        reportingLog.info(newMsg);
        log.info(newMsg);
    }

    @Override
    public void recordReportingRecordable(String taskName, ReportingRecordable reportingRecordable) {
        recordReportingMessage(taskName, reportingRecordable.toString());
    }

}
