package com.innowise.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.innowise.userservice.dto.response.UserWithCardsResponse;
import com.innowise.userservice.util.RedisKeyUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserCacheService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean redisAvailable;

    @Autowired
    public UserCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.objectMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        this.redisAvailable = checkRedisConnection();

        if (redisAvailable) {
            log.info("Redis is available, caching is enabled");
        } else {
            log.warn("Redis is NOT available, caching will be disabled. The application will continue to run without caching.");
        }
    }

    private boolean checkRedisConnection() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equals(pong);
        } catch (Exception e) {
            log.warn("Failed to connect to Redis: {}", e.getMessage());
            return false;
        }
    }

    public Optional<UserWithCardsResponse> getCachedUser(UUID userId) {
        if (!redisAvailable) {
            log.debug("Redis is unavailable, skipping reading from cache");
            return Optional.empty();
        }

        String key = RedisKeyUtil.getUserKey(userId);
        log.debug("Search in cache by key: {}", key);

        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                log.debug("The cache does not contain the user: {}", userId);
                return Optional.empty();
            }

            UserWithCardsResponse user = objectMapper.readValue(json, UserWithCardsResponse.class);
            log.debug("User {} found in cache", userId);
            return Optional.of(user);
        } catch (Exception e) {
            log.error("Error reading user {} from cache: {}", userId, e.getMessage());
            return Optional.empty();
        }
    }

    public void cacheUser(UserWithCardsResponse user) {
        if (!redisAvailable) {
            log.debug("Redis is unavailable, skipping saving to cache");
            return;
        }

        if (user == null || user.getId() == null) {
            log.warn("Attempting to save null user in cache");
            return;
        }

        String key = RedisKeyUtil.getUserKey(user.getId());

        try {
            String json = objectMapper.writeValueAsString(user);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
            log.debug("User {} has been cached. TTL: {} minutes", user.getId(), CACHE_TTL.toMinutes());
        } catch (Exception e) {
            log.error("Error saving user {} to cache: {}", user.getId(), e.getMessage());
        }
    }

    public void evictUser(UUID userId) {
        if (!redisAvailable) {
            log.debug("Redis is unavailable, skipping cache clearing");
            return;
        }

        if (userId == null) {
            log.warn("Attempting to clear cache with null ID");
            return;
        }

        String key = RedisKeyUtil.getUserKey(userId);

        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("User {}'s cache has been cleared", userId);
            } else {
                log.debug("User {} not found in cache for clearing", userId);
            }
        } catch (Exception e) {
            log.error("Error clearing user {}'s cache: {}", userId, e.getMessage());
        }
    }

    public void updateCachedUser(UserWithCardsResponse user) {
        if (!redisAvailable) {
            log.debug("Redis is unavailable, skipping cache update");
            return;
        }

        if (user == null || user.getId() == null) {
            log.warn("Attempting to update cache with null user");
            return;
        }

        cacheUser(user);
        log.debug("User {}'s cache has been updated.", user.getId());
    }
}
