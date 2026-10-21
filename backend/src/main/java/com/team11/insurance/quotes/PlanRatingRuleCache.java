package com.team11.insurance.quotes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Thread-safe, bounded in-memory cache for parsed plan rating rules.
 * Keys by planId to satisfy Day 1 caching requirements and avoid redundant
 * JSON parsing/database lookups on high-frequency quote calculations.
 */
@Component
public class PlanRatingRuleCache {

    private static final Logger log = LoggerFactory.getLogger(PlanRatingRuleCache.class);
    private static final int MAX_ENTRIES = 500;

    private final ConcurrentHashMap<Long, MotorRatingRules> cache = new ConcurrentHashMap<>();

    /**
     * Retrieves cached rating rules for the plan, or computes them using the provided loader.
     * Thread-safe via computeIfAbsent.
     */
    public MotorRatingRules getOrLoad(Long planId, Function<Long, MotorRatingRules> loader) {
        if (planId == null) {
            return null;
        }

        // Bound cache size to prevent unbounded memory growth
        if (cache.size() >= MAX_ENTRIES && !cache.containsKey(planId)) {
            log.warn("PlanRatingRuleCache reached max size ({}). Evicting all entries to enforce bound.", MAX_ENTRIES);
            cache.clear();
        }

        return cache.computeIfAbsent(planId, id -> {
            log.debug("PlanRatingRuleCache miss for planId={}. Loading rules...", id);
            return loader.apply(id);
        });
    }

    /**
     * Evicts cached rules for a specific plan.
     */
    public void invalidate(Long planId) {
        if (planId != null) {
            cache.remove(planId);
            log.debug("PlanRatingRuleCache invalidated for planId={}", planId);
        }
    }

    /**
     * Clears all cached rules across all plans.
     */
    public void invalidateAll() {
        cache.clear();
        log.debug("PlanRatingRuleCache cleared.");
    }

    /**
     * Returns the number of currently cached plans.
     */
    public int size() {
        return cache.size();
    }
}
