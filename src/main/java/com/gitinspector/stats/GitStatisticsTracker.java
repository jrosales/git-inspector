package com.gitinspector.stats;

import org.apache.commons.lang.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aids in the tracking of statistics on repositories and organizations as the git inspector tasks are iterating
 * over repositories. Each task should instantiate its OWN INSTANCE of this class.
 */
public class GitStatisticsTracker {

    private String statisticName;

    private Map<String, Integer> orgNameToTotalHitsMap = new HashMap<>();

    private Map<String, Integer> orgNameToPositiveHitsMap = new HashMap<>();

    private Map<String, Integer> repoNameToTotalHitsMap = new HashMap<>();

    private Map<String, Integer> repoNameToPositiveHitsMap = new HashMap<>();

    public GitStatisticsTracker(String statisticName) {
        this.statisticName = statisticName;
    }

    public void addHitToOrg(String orgName, boolean isPositiveHit) {
        incrementMapValue(orgNameToTotalHitsMap, orgName);
        if (isPositiveHit) {
            incrementMapValue(orgNameToPositiveHitsMap, orgName);
        }
    }

    public void addHitToRepo(String repoFullName, boolean isPositiveHit) {
        incrementMapValue(repoNameToTotalHitsMap, repoFullName);
        if (isPositiveHit) {
            incrementMapValue(repoNameToPositiveHitsMap, repoFullName);
        }
        addHitToOrg(StringUtils.substringBefore(repoFullName, "/"), isPositiveHit);
    }

    public String getStatisticName() {
        return statisticName;
    }

    public List<String> getAllOrgsWithHits() {
        return new ArrayList<>(orgNameToTotalHitsMap.keySet());
    }

    public int getTotalHits(StatsLevel statsLevel, String name) {
        switch (statsLevel) {
        case ORG_LEVEL:
            return getTotalHitsForOrg(name);
        case REPOSITORY_LEVEL:
            return getTotalHitsForRepo(name);
        default:
            throw new RuntimeException("stats level not recognized: " + statsLevel);
        }
    }

    public int getTotalHitsForOrg(String orgName) {
        return orgNameToTotalHitsMap.containsKey(orgName) ? orgNameToTotalHitsMap.get(orgName) : 0;
    }

    public int getTotalHitsForRepo(String repoFullName) {
        return repoNameToTotalHitsMap.containsKey(repoFullName) ? repoNameToTotalHitsMap.get(repoFullName) : 0;
    }

    public int getPositiveHits(StatsLevel statsLevel, String name) {
        switch (statsLevel) {
        case ORG_LEVEL:
            return getPositiveHitsForOrg(name);
        case REPOSITORY_LEVEL:
            return getPositiveHitsForRepo(name);
        default:
            throw new RuntimeException("stats level not recognized: " + statsLevel);
        }
    }

    public int getPositiveHitsForOrg(String orgName) {
        return orgNameToPositiveHitsMap.containsKey(orgName) ? orgNameToPositiveHitsMap.get(orgName) : 0;
    }

    public int getPositiveHitsForRepo(String repoFullName) {
        return repoNameToPositiveHitsMap.containsKey(repoFullName) ? repoNameToPositiveHitsMap.get(repoFullName) : 0;
    }

    public int getPercentageOfPositiveHits(StatsLevel statsLevel, String name) {
        switch (statsLevel) {
        case ORG_LEVEL:
            return getPercentageOfPositiveHitsForOrg(name);
        case REPOSITORY_LEVEL:
            return getPercentageOfPositiveHitsForRepo(name);
        default:
            throw new RuntimeException("stats level not recognized: " + statsLevel);
        }
    }

    public int getPercentageOfPositiveHitsForOrg(String orgName) {
        return calculatePercentPositiveHits(getPositiveHitsForOrg(orgName), getTotalHitsForOrg(orgName));
    }

    public int getPercentageOfPositiveHitsForRepo(String repoFullName) {
        return calculatePercentPositiveHits(getPositiveHitsForRepo(repoFullName), getTotalHitsForRepo(repoFullName));
    }

    private int calculatePercentPositiveHits(Integer positiveHits, Integer totalHits) {
        if (positiveHits == null || totalHits == null || positiveHits == 0 || totalHits == 0) {
            return 0;
        }

        return new BigDecimal(positiveHits)
            .divide(new BigDecimal(totalHits), 2, BigDecimal.ROUND_HALF_UP)
            .multiply(new BigDecimal(100))
            .setScale(0, BigDecimal.ROUND_HALF_UP)
            .intValue();
    }

    private void incrementMapValue(Map<String, Integer> map, String key) {
        Integer value = map.get(key);
        value = value == null ? 1 : value + 1;
        map.put(key, value);
    }
}
