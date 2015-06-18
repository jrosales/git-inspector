/*
 * Copyright Homeaway, Inc 2015-Present. All Rights Reserved.
 * No unauthorized use of this software.
 */
package com.gitinspector;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.kohsuke.github.GHOrganization;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests the TargetRepositories class
 */
public class TargetRepositoriesTest {

    @Test
    public void testTargetRepositories() throws IOException {
        final GitHub gitHub = mock(GitHub.class);

        final GHRepository omapi = mock(GHRepository.class);
        when(omapi.getFullName()).thenReturn("OMDev/omapi");

        final GHRepository oms = mock(GHRepository.class);
        when(oms.getFullName()).thenReturn("OMDev/oms");

        final GHRepository epic = mock(GHRepository.class);
        when(epic.getFullName()).thenReturn("payment-services/epic");

        GHOrganization omDevOrg = mock(GHOrganization.class);
        when(omDevOrg.getRepositories()).thenReturn(ImmutableMap.of("omapi", omapi, "oms", oms));
        when(gitHub.getOrganization("OMDev")).thenReturn(omDevOrg);

        GHOrganization paymentServicesOrg = mock(GHOrganization.class);
        when(paymentServicesOrg.getRepositories()).thenReturn(ImmutableMap.of("epic", epic));
        when(gitHub.getOrganization("payment-services")).thenReturn(paymentServicesOrg);

        TargetRepositories targetRepositories = new TargetRepositories(gitHub,
            Arrays.asList("OMDev", "payment-services"),
            new ArrayList<String>());

        // validate the full list of repos and org names is returned and that there are no out-of-scope repos
        validateRepoList(targetRepositories.getTargetedRepositories(), omapi, oms, epic);
        validateStringList(targetRepositories.getGitOrgNamesInScope(), "OMDev", "payment-services");
        validateStringList(targetRepositories.getRepositoryNamesOutOfScope());

        // exclude omapi and re-validate everything
        targetRepositories.excludeRepositoryFromScope("OMDev/omapi");
        validateRepoList(targetRepositories.getTargetedRepositories(), oms, epic);
        validateStringList(targetRepositories.getGitOrgNamesInScope(), "OMDev", "payment-services");
        validateStringList(targetRepositories.getRepositoryNamesOutOfScope(), "OMDev/omapi");

        // re-add omapi and re-validate everything
        targetRepositories.cancelRepositoryExclusion("OMDev/omapi");
        validateRepoList(targetRepositories.getTargetedRepositories(), omapi, oms, epic);
        validateStringList(targetRepositories.getGitOrgNamesInScope(), "OMDev", "payment-services");
        validateStringList(targetRepositories.getRepositoryNamesOutOfScope());
    }

    private void validateStringList(List<String> testStringList, String... expectedStringList) {
        if (expectedStringList == null) {
            assertTrue(testStringList == null || testStringList.isEmpty());
            return;
        }

        assertEquals(expectedStringList.length, testStringList.size());
        for (String expectedOrgName : expectedStringList) {
            assertTrue(testStringList.contains(expectedOrgName));
        }
    }

    private void validateRepoList(List<GHRepository> testRepoList, GHRepository... expectedRepos) {
        assertEquals(expectedRepos.length, testRepoList.size());
        for (GHRepository expectedRepo : expectedRepos) {
            assertTrue(testRepoList.contains(expectedRepo));
        }
    }
}
