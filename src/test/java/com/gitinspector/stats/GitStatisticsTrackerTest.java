package com.gitinspector.stats;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the GitStatisticsTracker class.
 */
public class GitStatisticsTrackerTest {

    @Test
    public void testEmptyStats() {
        final String statName = "testEmptyStats";
        GitStatisticsTracker tracker = new GitStatisticsTracker(statName);
        assertEquals(statName, tracker.getStatisticName());

        // nothing being tracked yet; make sure everything returns 0
        assertTrue(tracker.getAllOrgsWithHits().isEmpty());
        assertEquals(0, tracker.getPositiveHitsForOrg("bogus"));
        assertEquals(0, tracker.getTotalHitsForOrg("bogus"));
        assertEquals(0, tracker.getPercentageOfPositiveHitsForOrg("bogus"));
        assertEquals(0, tracker.getPositiveHitsForRepo("bogus"));
        assertEquals(0, tracker.getTotalHitsForRepo("bogus"));
        assertEquals(0, tracker.getPercentageOfPositiveHitsForRepo("bogus"));
    }

    @Test
    public void testRepoAndOrgHits() {
        GitStatisticsTracker tracker = new GitStatisticsTracker("testRepoHits");

        final String omDevOrg = "OMDev";
        final String paymentServicesOrg = "payment-services";
        final String adminToolsOrg = "admin-tools";

        final String omapiRepo = omDevOrg + "/omapi";
        final String omsRepo = omDevOrg + "/oms";
        final String bogusOmDevRepo = omDevOrg + "/bogus";
        final String epicRepo = paymentServicesOrg + "/epic";

        // log a positive hit to OMDev/omapi
        tracker.addHitToRepo(omapiRepo, true);
        validateListOfOrgsWithHits(tracker, omDevOrg);
        validateOrgStats(tracker, omDevOrg, 1, 1, 100);
        validateRepoStats(tracker, omapiRepo, 1, 1, 100);
        validateRepoStats(tracker, bogusOmDevRepo, 0, 0, 0);

        // log a negative hit against OMDev/omapi
        tracker.addHitToRepo(omapiRepo, false);
        validateListOfOrgsWithHits(tracker, omDevOrg);
        validateOrgStats(tracker, omDevOrg, 1, 2, 50);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);

        // log a positive hit to OMDev/oms
        tracker.addHitToRepo(omsRepo, true);
        validateListOfOrgsWithHits(tracker, omDevOrg);
        validateOrgStats(tracker, omDevOrg, 2, 3, 67);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 1, 100);

        // log a negative hit to OMDev/oms
        tracker.addHitToRepo(omsRepo, false);
        validateListOfOrgsWithHits(tracker, omDevOrg);
        validateOrgStats(tracker, omDevOrg, 2, 4, 50);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 2, 50);

        // log a positive hit to a payment-services/epic
        tracker.addHitToRepo(epicRepo, true);
        validateListOfOrgsWithHits(tracker, omDevOrg, paymentServicesOrg);
        validateOrgStats(tracker, omDevOrg, 2, 4, 50);
        validateOrgStats(tracker, paymentServicesOrg, 1, 1, 100);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 2, 50);
        validateRepoStats(tracker, epicRepo, 1, 1, 100);

        // log a positive hit to the payment-services org (no repo)
        tracker.addHitToOrg(paymentServicesOrg, true);
        validateListOfOrgsWithHits(tracker, omDevOrg, paymentServicesOrg);
        validateOrgStats(tracker, omDevOrg, 2, 4, 50);
        validateOrgStats(tracker, paymentServicesOrg, 2, 2, 100);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 2, 50);
        validateRepoStats(tracker, epicRepo, 1, 1, 100);

        // log a negative hit to the admin-tools org (no repo)
        tracker.addHitToOrg(adminToolsOrg, false);
        validateListOfOrgsWithHits(tracker, omDevOrg, paymentServicesOrg, adminToolsOrg);
        validateOrgStats(tracker, omDevOrg, 2, 4, 50);
        validateOrgStats(tracker, paymentServicesOrg, 2, 2, 100);
        validateOrgStats(tracker, adminToolsOrg, 0, 1, 0);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 2, 50);
        validateRepoStats(tracker, epicRepo, 1, 1, 100);

        // log a positive hit to the admin-tools org (no repo)
        tracker.addHitToOrg(adminToolsOrg, true);
        validateListOfOrgsWithHits(tracker, omDevOrg, paymentServicesOrg, adminToolsOrg);
        validateOrgStats(tracker, omDevOrg, 2, 4, 50);
        validateOrgStats(tracker, paymentServicesOrg, 2, 2, 100);
        validateOrgStats(tracker, adminToolsOrg, 1, 2, 50);
        validateRepoStats(tracker, omapiRepo, 1, 2, 50);
        validateRepoStats(tracker, omsRepo, 1, 2, 50);
        validateRepoStats(tracker, epicRepo, 1, 1, 100);
    }

    private void validateOrgStats(GitStatisticsTracker tracker, String orgName, int positiveHits, int totalHIts,
                                  int percentPositive) {
        assertEquals(positiveHits, tracker.getPositiveHitsForOrg(orgName));
        assertEquals(positiveHits, tracker.getPositiveHits(StatsLevel.ORG_LEVEL, orgName));
        assertEquals(totalHIts, tracker.getTotalHitsForOrg(orgName));
        assertEquals(totalHIts, tracker.getTotalHits(StatsLevel.ORG_LEVEL, orgName));
        assertEquals(percentPositive, tracker.getPercentageOfPositiveHitsForOrg(orgName));
        assertEquals(percentPositive, tracker.getPercentageOfPositiveHits(StatsLevel.ORG_LEVEL, orgName));
    }

    private void validateRepoStats(GitStatisticsTracker tracker, String repoName, int positiveHits, int totalHIts,
                                   int percentPositive) {
        assertEquals(positiveHits, tracker.getPositiveHitsForRepo(repoName));
        assertEquals(totalHIts, tracker.getTotalHitsForRepo(repoName));
        assertEquals(totalHIts, tracker.getTotalHits(StatsLevel.REPOSITORY_LEVEL, repoName));
        assertEquals(percentPositive, tracker.getPercentageOfPositiveHitsForRepo(repoName));
        assertEquals(percentPositive, tracker.getPercentageOfPositiveHits(StatsLevel.REPOSITORY_LEVEL, repoName));
    }

    private void validateListOfOrgsWithHits(GitStatisticsTracker tracker, String... orgNames) {
        final List<String> allOrgsWithHits = tracker.getAllOrgsWithHits();

        assertEquals(orgNames.length, allOrgsWithHits.size());

        for (String orgName : orgNames) {
            assertTrue(allOrgsWithHits.contains(orgName));
        }
    }
}
