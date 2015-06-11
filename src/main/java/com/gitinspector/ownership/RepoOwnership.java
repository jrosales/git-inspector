package com.gitinspector.ownership;

import com.gitinspector.domain.GitUser;

import java.util.List;

/**
 * Facilitates figuring out who owns each repository and who the approved code reviewers are.
 */
public interface RepoOwnership {
    /**
     * @param repoFullName the full name of the repository whose ownership information is being requested (e.g. OMDev/omapi)
     * @return a GitUser representing the repo owner or null if the information could not be found
     */
    GitUser getRepositoryOwner(String repoFullName);

    /**
     * @param repoFullName the full name of the repository whose reviewer information is being requested (e.g. OMDev/omapi)
     * @return a list of GitUsers representing the reviewers for the repository or an EMPTY LIST if the
     * information could not be found
     */
    List<GitUser> getRepositoryReviewers(String repoFullName);
}
