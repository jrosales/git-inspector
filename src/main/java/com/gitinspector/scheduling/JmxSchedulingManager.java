package com.gitinspector.scheduling;

import org.apache.commons.lang.exception.ExceptionUtils;
//import org.springframework.jmx.export.annotation.ManagedAttribute;
//import org.springframework.jmx.export.annotation.ManagedOperation;
//import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

// TODO: consider NOT having this be a JMX enabled class. Design it as a standalone manager. In our webapp we can create a JMX enabled
// class that delegates to this guy
// if this is the only reason we use spring, should we just dump spring altogether?

/**
 * Enables JMX management of git inspector scheduled tasks.
 * This class is built explicitly for JMX exposure.
 * !!! Do not call these methods from code !!!
 */
//@ManagedResource(description = "Enables JMX management of git inspector scheduled tasks.")
public class JmxSchedulingManager {
    private TaskScheduler scheduler;
    private Map<String, ScheduledTaskInfo> scheduledTaskInfoMap = new HashMap<>();

    /**
     * @param scheduler a spring TaskScheduler that can be used to schedule tasks
     * @param scheduledTasks a Map of Runnables to the Cron expressions that should be used to schedule them
     */
    public JmxSchedulingManager(TaskScheduler scheduler, Map<Runnable, String> scheduledTasks) {
        this.scheduler = scheduler;

        for (Runnable task : scheduledTasks.keySet()) {
            scheduleTask(scheduler, scheduledTasks.get(task), task);
        }
    }

//    @ManagedAttribute
    public String getScheduledTasks() {
        StringBuilder stringBuilder = new StringBuilder();

        for (ScheduledTaskInfo scheduledTaskInfo : scheduledTaskInfoMap.values()) {
            stringBuilder.append(scheduledTaskInfo.getTask().getClass().getSimpleName())
                    .append(":").append(scheduledTaskInfo.getCronExpression())
                    .append("\n");
        }

        return stringBuilder.toString();
    }

//    @ManagedOperation
    public String updateSchedule(String className, String cronExpression) {
        try {
            if (!scheduledTaskInfoMap.containsKey(className)) {
                return "Could not find scheduled task with className " + className;
            }

            // retrieve the ScheduledTaskInfo associated with the class name
            ScheduledTaskInfo scheduledTaskInfo = scheduledTaskInfoMap.get(className);
            // cancel the task using the saved ScheduledFuture
            scheduledTaskInfo.getScheduledFuture().cancel(true);
            // reschedule the task using the new cron expression
            scheduleTask(scheduler, cronExpression, scheduledTaskInfo.getTask());

            return "Scheduling " + className + " using expression " + cronExpression;
        } catch (Exception e) {
            return ExceptionUtils.getFullStackTrace(e);
        }
    }

//    @ManagedOperation
    public String executeTaskImmediately(String className) {
        try {
            if (!scheduledTaskInfoMap.containsKey(className)) {
                return "Could not find scheduled task with className " + className;
            }

            scheduledTaskInfoMap.get(className).getTask().run();

            return "Task complete";
        } catch (Exception e) {
            return ExceptionUtils.getFullStackTrace(e);
        }
    }

//    @ManagedOperation
    public String executeAllTasksImmediately() {
        try {
            for (ScheduledTaskInfo scheduledTaskInfo : scheduledTaskInfoMap.values()) {
                scheduledTaskInfo.getTask().run();
            }
        } catch (Exception e) {
            return ExceptionUtils.getFullStackTrace(e);
        }

        return "All tasks complete";
    }

    /**
     * Schedule a task.
     *
     * @param scheduler the Spring TaskScheduler to use to schedule the task
     * @param cronExpression the cron expression representing when the task should run
     * @param task a Runnable representing the task to be executed
     */
    private void scheduleTask(TaskScheduler scheduler, String cronExpression, Runnable task) {
        // schedule the task using the cron expression and save off the ScheduledFuture in case we
        // ever need to cancel the task or reschedule it using a different cron expression
        final ScheduledFuture<?> scheduledFuture = scheduler.schedule(task, new CronTrigger(cronExpression));
        scheduledTaskInfoMap.put(task.getClass().getSimpleName(),
                                 new ScheduledTaskInfo(task, cronExpression, scheduledFuture));
    }

    private class ScheduledTaskInfo {
        private Runnable task;
        private String cronExpression;
        private ScheduledFuture scheduledFuture;

        public ScheduledTaskInfo(Runnable task, String cronExpression, ScheduledFuture scheduledFuture) {
            this.task = task;
            this.cronExpression = cronExpression;
            this.scheduledFuture = scheduledFuture;
        }

        public Runnable getTask() {
            return task;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public ScheduledFuture getScheduledFuture() {
            return scheduledFuture;
        }
    }

}
