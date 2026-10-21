package com.team11.insurance.quotes;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlanRatingRuleCacheTest {

    private PlanRatingRuleCache cache;

    @BeforeEach
    void setUp() {
        cache = new PlanRatingRuleCache();
    }

    @Test
    @DisplayName("Cache miss invokes loader; repeated access returns cached instance without re-loading")
    void testCacheHitAvoidsReloading() {
        AtomicInteger loaderCalls = new AtomicInteger(0);

        MotorRatingRules rules1 = cache.getOrLoad(1L, id -> {
            loaderCalls.incrementAndGet();
            return new MotorRatingRules(new BigDecimal("0.02"), 6, 10, new BigDecimal("0.05"), Map.of("ZERO_DEP", new BigDecimal("1080.00")));
        });

        MotorRatingRules rules2 = cache.getOrLoad(1L, id -> {
            loaderCalls.incrementAndGet();
            return new MotorRatingRules();
        });

        assertEquals(1, loaderCalls.get(), "Loader should only be invoked once for the same planId");
        assertSame(rules1, rules2, "Cached instance must be reused");
        assertEquals(1, cache.size());
    }

    @Test
    @DisplayName("Invalidating a plan removes it and forces fresh reload")
    void testInvalidationForcesReload() {
        AtomicInteger loaderCalls = new AtomicInteger(0);

        cache.getOrLoad(1L, id -> {
            loaderCalls.incrementAndGet();
            return new MotorRatingRules();
        });
        assertEquals(1, loaderCalls.get());

        cache.invalidate(1L);
        assertEquals(0, cache.size());

        cache.getOrLoad(1L, id -> {
            loaderCalls.incrementAndGet();
            return new MotorRatingRules();
        });
        assertEquals(2, loaderCalls.get(), "Loader must be called again after invalidation");
    }

    @Test
    @DisplayName("InvalidateAll clears all cached plans")
    void testInvalidateAllClearsCache() {
        cache.getOrLoad(1L, id -> new MotorRatingRules());
        cache.getOrLoad(2L, id -> new MotorRatingRules());
        assertEquals(2, cache.size());

        cache.invalidateAll();
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("Cache is thread-safe under concurrent access across multiple threads")
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger loaderCalls = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    cache.getOrLoad(1L, id -> {
                        loaderCalls.incrementAndGet();
                        return new MotorRatingRules();
                    });
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(1, loaderCalls.get(), "Concurrent requests must safely serialize so loader runs only once");
        assertEquals(1, cache.size());
    }
}
