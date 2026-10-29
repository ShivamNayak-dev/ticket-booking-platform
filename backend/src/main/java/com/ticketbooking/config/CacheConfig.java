package com.ticketbooking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache setup. Every cache has its own time-to-live (TTL):
 *   event         - single event details
 *   eventList     - paginated / filtered event lists (short TTL, changes often)
 *   userBookings  - a user's tickets
 * The TTLs come from application.properties so they can be tuned without code changes.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String EVENT_CACHE = "event";
    public static final String EVENT_LIST_CACHE = "eventList";
    public static final String USER_BOOKINGS_CACHE = "userBookings";

    @Value("${app.cache.event-ttl-minutes}")
    private long eventTtlMinutes;

    @Value("${app.cache.event-list-ttl-minutes}")
    private long eventListTtlMinutes;

    @Value("${app.cache.booking-ttl-minutes}")
    private long bookingTtlMinutes;

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // The cached DTOs implement Serializable, so the plain JDK serializer is enough.
        // (JSON would need extra type information to turn the data back into our DTO classes.)
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new JdkSerializationRedisSerializer()));

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(EVENT_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(eventTtlMinutes)));
        cacheConfigs.put(EVENT_LIST_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(eventListTtlMinutes)));
        cacheConfigs.put(USER_BOOKINGS_CACHE, defaultConfig.entryTtl(Duration.ofMinutes(bookingTtlMinutes)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
