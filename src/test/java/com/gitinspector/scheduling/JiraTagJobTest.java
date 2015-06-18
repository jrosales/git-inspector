package com.gitinspector.scheduling;

import com.gitinspector.TargetRepositories;
import com.gitinspector.ownership.RepoOwnership;
import com.gitinspector.recording.TaskMessageRecorder;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created by skhatri
 */
public class JiraTagJobTest {

    @Test
    public void testJiraTagRegexPattern() throws IOException, URISyntaxException {
        final TargetRepositories targetRepositories = mock(TargetRepositories.class);
        final TaskMessageRecorder taskMessageRecorder = mock(TaskMessageRecorder.class);
        final RepoOwnership repoOwnership = mock(RepoOwnership.class);

        JiraTagJob jiraTagJob = new JiraTagJob(targetRepositories, taskMessageRecorder, repoOwnership, 7);

        assertTrue(jiraTagJob.isCommitValid("PAY-1567.  Fixed payments with existing mandates and PIs."));
        assertTrue(jiraTagJob.isCommitValid("OPP-2165 validate refund form for REFUND_BY_TRANSFER"));
        assertTrue(jiraTagJob.isCommitValid("PAY-1457.  Adding mandate to FTs."));
        assertTrue(jiraTagJob.isCommitValid("GC-1415: upgaded to base-pom-20, fixed old dependencies"));
        assertTrue(
            jiraTagJob.isCommitValid("OPP-2273: Issued By on an invoice after order cancellation is HAOrderMgmt [Reviewed by: dcorutiu]"));
        assertTrue(jiraTagJob.isCommitValid("[OPP-188] Order line item CSV export [reviewed by dcorutiu]"));
        assertTrue(jiraTagJob.isCommitValid("OPP-111: threshold management endpoints (create/update/read) [Reviewed by: skarivelithara]"));
        assertTrue(jiraTagJob.isCommitValid("OPP-1082: Apply the chargeback/NSF"));
        assertTrue(jiraTagJob.isCommitValid("GC-1463: Upgrade New Relic to 3.16.1"));
        assertTrue(jiraTagJob.isCommitValid("(GC-1561): Make SEPA option the default"));
        assertTrue(jiraTagJob.isCommitValid("GC-1424- code review feedback"));
        assertTrue(jiraTagJob.isCommitValid("[maven-release-plugin] prepare for next development iteration"));
        assertTrue(jiraTagJob.isCommitValid("[grunt-release-plugin] Released pay-prefs-ui 1.7.6"));
        assertTrue(jiraTagJob.isCommitValid("PAY-1541 EPIC: Enable SEPA for Offline Refunds\n"
            + "Add new FinancialTransaction.FinancialTransactionType.SEPA_RETURN (a credit)\n"
            + "Add new FtActionType.ActionType.SEPA_RETURN"));

        assertFalse(jiraTagJob.isCommitValid("Updating failing test in QuickBuild."));
        assertFalse(jiraTagJob.isCommitValid("set version back to 1.5-SNAPSHOT"));
        assertFalse(jiraTagJob.isCommitValid("use the closed dto dependency"));
        assertFalse(jiraTagJob.isCommitValid("Pass the actual error from omapi to the clients [Reviewed by Daniel]"));
        assertFalse(jiraTagJob.isCommitValid("bumped version to 4.3.0-SNAPSHOT"));
        assertFalse(jiraTagJob.isCommitValid(
            "JMX switch so that any of the processor methods that are triggered by rabbit events can be triggered manually."));
        assertFalse(jiraTagJob.isCommitValid("@Ignore on DateUtilsTest and LocalDateTimeUtilsTest. They are broken on Sunday"));
        assertFalse(jiraTagJob.isCommitValid("Ensuring window.logger exists"));
        assertFalse(jiraTagJob.isCommitValid("OMDev/global-cart-thin-ui-endpoint"));
    }
}
