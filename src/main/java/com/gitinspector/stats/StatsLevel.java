package com.gitinspector.stats;

/**
 *
 * An enum to differentiate between the different "levels" of statistics. In other words, some stats are tracked
 * at the repo level while others are tracked at the org level.
 */
public enum StatsLevel {
    REPOSITORY_LEVEL, ORG_LEVEL
}
