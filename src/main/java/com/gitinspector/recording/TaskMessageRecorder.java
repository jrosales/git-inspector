package com.gitinspector.recording;

import com.gitinspector.domain.recordable.ReportingRecordable;

/**
 * Abstracts the actual recording of messages from our scheduled tasks.
 * This abstraction should allow us to switch from recording to log files to something else in the future.
 */
public interface TaskMessageRecorder {

    /**
     * Record a message related to a state change in the task itself (starting, stopping, etc)
     *
     * @param taskName the task generating the message
     * @param msg      the message to be recorded
     */
    void recordStateRelatedMessage(String taskName, String msg);

    /**
     * Record an error (i.e. an exception or failure)
     *
     * @param taskName the task generating the message
     * @param msg      the error message to be recorded
     */
    void recordError(String taskName, String msg);

    /**
     * Record a reporting message as a simple string.
     *
     * @param taskName the task generating the message
     * @param msg      the message to be recorded
     */
    void recordReportingMessage(String taskName, String msg);

    /**
     * Record a ReportingRecordable object.
     *
     * @param taskName            the task generating the message
     * @param reportingRecordable the ReportingRecordable to be recorded
     */
    void recordReportingRecordable(String taskName, ReportingRecordable reportingRecordable);
}
