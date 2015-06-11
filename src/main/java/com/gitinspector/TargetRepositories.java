package com.gitinspector;

import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Encapsulates the list of repositories to be evaluated by the git inspector.
 */
@ManagedResource(description = "Encapsulates the list of repositories to be evaluated by the git inspector.")
public class TargetRepositories {

    private GitHub gitHub;

    private List<String> gitOrgsInScope;

    private List<String> gitReposOutOfScope;

    public TargetRepositories(GitHub gitHub, List<String> gitOrgsInScope, List<String> gitReposOutOfScope) {
        this.gitHub = gitHub;
        this.gitOrgsInScope = new ArrayList<>(gitOrgsInScope);
        this.gitReposOutOfScope = new ArrayList<>(gitReposOutOfScope);
    }

    @ManagedOperation
    public List<GHRepository> getTargetedRepositories() throws IOException {
        List<GHRepository> targetedRepos = new ArrayList<>();

        for (String gitOrgName : gitOrgsInScope) {
            final Collection<GHRepository> reposForOrg = gitHub.getOrganization(gitOrgName).getRepositories().values();
            for (GHRepository repo : reposForOrg) {
                if (!gitReposOutOfScope.contains(repo.getFullName())) {
                    targetedRepos.add(repo);
                }
            }
        }

        return targetedRepos;
    }

    @ManagedAttribute
    public List<String> getGitOrgNamesInScope() {
        return gitOrgsInScope;
    }

    @ManagedAttribute
    public List<String> getRepositoryNamesOutOfScope() {
        return gitReposOutOfScope;
    }

    @ManagedOperation
    public void excludeRepositoryFromScope(String repositoryFullName) {
        if (!StringUtils.isBlank(repositoryFullName) && !gitReposOutOfScope.contains(repositoryFullName)) {
            gitReposOutOfScope.add(repositoryFullName);
        }
    }

    @ManagedOperation
    public void cancelRepositoryExclusion(String repositoryFullName) {
        if (!StringUtils.isBlank(repositoryFullName) && gitReposOutOfScope.contains(repositoryFullName)) {
            gitReposOutOfScope.remove(repositoryFullName);
        }
    }
}
