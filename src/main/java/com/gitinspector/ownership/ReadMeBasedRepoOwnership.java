package com.gitinspector.ownership;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.gitinspector.domain.GitUser;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.github.GHContent;
import org.kohsuke.github.GitHub;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static com.gitinspector.ownership.ReadMeBasedRepoOwnership.OwnershipInfo.getEmptyOwnershipInfo;

/**
 * A RepoOwnership implementation that looks for ownership information inside of README.md files.
 * Uses a TTL cache to avoid having to keep re-reading the readme file over and over again.
 * The expected format of the ownership section of the readme is:
 * {@code
 * #Ownership#
 * Owner: <owner username> (<owner email>)<br/>
 * Code Reviewers: <reviewer1 username> (<reviewer1 email>),<reviewer2 username> (<reviewer2 email>)...<br/>
 * }
 */
public class ReadMeBasedRepoOwnership implements RepoOwnership {
    private static final Logger log = LoggerFactory.getLogger(ReadMeBasedRepoOwnership.class);
    private static final int CACHE_ENTRY_HOURS_TTL = 12;
    private static final String README_FILENAME = "README.MD";
    public static final String ANY = "any";

    private GitHub gitHub;
    private LoadingCache<String, OwnershipInfo> ownershipInfoCache;

    public ReadMeBasedRepoOwnership(GitHub gitHub) {
        this.gitHub = gitHub;

        // build a TTL cache for the ownership information keyed on the full name of the repositories
        ownershipInfoCache = CacheBuilder.newBuilder()
                .expireAfterWrite(CACHE_ENTRY_HOURS_TTL, TimeUnit.HOURS)
                .build(new CacheLoader<String, OwnershipInfo>() {
                    @Override
                    public OwnershipInfo load(String repoFullName) throws Exception {
                        return getGitOwnershipInfo(repoFullName);
                    }
                });
    }

    @Override
    public GitUser getRepositoryOwner(String repoFullName) {
        try {
            return ownershipInfoCache.get(repoFullName).getOwner();
        } catch (ExecutionException e) {
            log.error("Error accessing ownership cache for repo: " + repoFullName, e);
            return null;
        }
    }

    @Override
    public List<GitUser> getRepositoryReviewers(String repoFullName) {
        try {
            return ownershipInfoCache.get(repoFullName).getReviewers();
        } catch (ExecutionException e) {
            log.error("Error accessing ownership cache for repo: " + repoFullName, e);
            return null;
        }
    }

    private OwnershipInfo getGitOwnershipInfo(String repoFullName) {
        try {
            // search for a README.md file and, if we find one, attempt to parse out the ownership information from it
            final List<GHContent> directoryContent = gitHub.getRepository(repoFullName).getDirectoryContent("");
            for (GHContent content : directoryContent) {
                if (README_FILENAME.equalsIgnoreCase(content.getName())) {
                    return parseOwnershipInfo(repoFullName, content.getContent());
                }
            }
        } catch(FileNotFoundException e) {
            log.warn("Cannot retrieve contents of empty repository {}.", repoFullName);
        } catch(Exception e) {
            log.error("Error getting repository owner from README.md file. Repo: " + repoFullName, e);
            return getEmptyOwnershipInfo();
        }

        return getEmptyOwnershipInfo();
    }

    private OwnershipInfo parseOwnershipInfo(String repoFullName, String readMeContent) {
        final String[] lines = StringUtils.split(readMeContent, "\n");
        for (int i = 0; i < lines.length; i++) {
            String ownershipLine = lines[i];
            if (ownershipLine.toLowerCase().contains("#ownership#")) {
                // we found the ownership section, make sure there are at least 2 more lines in the file
                if (i + 2 < lines.length) {
                    // the owner line should look like: "Owner: bcorbett (bcorbett@homeaway.com)"
                    String ownerLine = lines[i + 1];
                    // only examine the portion after the colon "bcorbett (bcorbett@homeaway.com)"
                    ownerLine = StringUtils.substringAfter(ownerLine, ":");
                    GitUser owner = parseGitUserFromSection(repoFullName, ownerLine, false);

                    List<GitUser> reviewers = new ArrayList<>();
                    // the reviewers line should look like:
                    // "Code Reviewers: bcorbett (bcorbett@homeaway.com), skhatri (skhatri@homeaway.com)"
                    String reviewersLine = lines[i + 2];
                    // only examine the portion after the colon
                    // "bcorbett (bcorbett@homeaway.com), skhatri (skhatri@homeaway.com)"
                    reviewersLine = StringUtils.substringAfter(reviewersLine, ":");
                    // split based on the commas
                    final String[] reviewersParts = StringUtils.split(reviewersLine, ",");
                    for (String reviewerStr : reviewersParts) {
                        // each reviewer string should look like "bcorbett (bcorbett@homeaway.com)" or "any"
                        final GitUser reviewer = parseGitUserFromSection(repoFullName, reviewerStr, true);
                        reviewers.add(reviewer);
                    }

                    return new OwnershipInfo(owner, reviewers);
                }
            }
        }
        return getEmptyOwnershipInfo();
    }

    /**
     * Parse a GitUser from the provided string.
     *
     * @param repoFullName the fullname of the repository whose owernship info we are retrieving
     * @param gitUserStr   a String of the form <username> (<email>)</br> e.g. "bcorbett (bcorbett@homeaway.com)</br>"
     * @param allowAny     if true, "any" is an acceptable gitUserStr; if false, "any" is not accetable
     * @return a GitUser parsed from the provided string
     */
    private GitUser parseGitUserFromSection(String repoFullName, String gitUserStr, boolean allowAny) {
        gitUserStr = StringUtils.remove(gitUserStr, "<br/>");

        if (allowAny) {
            if (ANY.equalsIgnoreCase(StringUtils.trim(gitUserStr))) {
                return new GitUser(ANY, ANY);
            }
        }

        final String[] parts = StringUtils.split(gitUserStr);
        if (parts.length < 2) {
            log.error("Error while trying to read owernship from README.md in repo: " + repoFullName);
            return null;
        }
        String username = parts[0].trim();
        String emailAddress = StringUtils.substringBetween(parts[1], "(", ")");
        return new GitUser(username, emailAddress);
    }

    protected static class OwnershipInfo {
        private GitUser owner;
        private List<GitUser> reviewers;

        public static OwnershipInfo getEmptyOwnershipInfo() {
            return new OwnershipInfo(null, new ArrayList<GitUser>());
        }

        public OwnershipInfo(GitUser owner, List<GitUser> reviewers) {
            this.owner = owner;
            this.reviewers = reviewers;
        }

        public GitUser getOwner() {
            return owner;
        }

        public List<GitUser> getReviewers() {
            return reviewers;
        }
    }
}
