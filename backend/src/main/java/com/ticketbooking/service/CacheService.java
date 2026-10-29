package com.ticketbooking.service;

import com.ticketbooking.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Central place for removing stale entries from Redis after data changes.
 *
 * The eviction is delayed until the database transaction has really committed.
 * If we cleared the cache earlier, another request could read the old row from
 * the database and put it straight back into Redis before our commit finishes.
 */
@Service
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    private final CacheManager cacheManager;

    public CacheService(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    public void evictEvent(Long eventId) {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(CacheConfig.EVENT_CACHE);
            if (cache != null) {
                cache.evict(eventId);
            }
        });
    }

    public void evictAllEventLists() {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(CacheConfig.EVENT_LIST_CACHE);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    public void evictUserBookings(String email) {
        runAfterCommit(() -> {
            Cache cache = cacheManager.getCache(CacheConfig.USER_BOOKINGS_CACHE);
            if (cache != null) {
                cache.evict(email);
            }
        });
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeRun(action);
                }
            });
        } else {
            safeRun(action);
        }
    }

    // a Redis outage must not break a booking that is already saved in the database
    private void safeRun(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            log.warn("Could not evict cache entry: {}", ex.getMessage());
        }
    }
}
