package com.webapp.bankingportal.service;

import com.webapp.bankingportal.type.CacheKeyType;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, Object> localCache = new ConcurrentHashMap<>();
    private boolean redisAvailable = true;

    public CacheServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        // Kiểm tra Redis connection
        checkRedisConnection();
    }

    /**
     * Kiểm tra Redis connection
     */
    private void checkRedisConnection() {
        try {
            redisTemplate.getConnectionFactory().getConnection().ping();
            redisAvailable = true;
            log.info("Redis connection established successfully");
        } catch (Exception e) {
            redisAvailable = false;
            log.warn("Redis is not available, falling back to local cache. Error: {}", e.getMessage());
        }
    }

    @Override
    public boolean exists(CacheKeyType cacheKeyType, String... keyArguments) {
        try {
            String key = acquireKey(cacheKeyType, keyArguments);
            
            if (redisAvailable) {
                Boolean exists = redisTemplate.hasKey(key);
                return exists != null && exists;
            } else {
                return localCache.containsKey(key);
            }
        } catch (Exception e) {
            log.error("Error checking key existence in cache: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<String> get(CacheKeyType cacheKeyType, String... keyArguments) {
        try {
            String key = acquireKey(cacheKeyType, keyArguments);
            
            if (redisAvailable) {
                Object value = redisTemplate.opsForValue().get(key);
                return Optional.ofNullable(value).map(Object::toString);
            } else {
                Object value = localCache.get(key);
                return Optional.ofNullable(value).map(Object::toString);
            }
        } catch (Exception e) {
            log.error("Error retrieving value from cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public <T> Optional<T> get(CacheKeyType cacheKeyType, Class<T> clazz, String... keyArguments) {
        try {
            String key = acquireKey(cacheKeyType, keyArguments);
            Object value;
            
            if (redisAvailable) {
                value = redisTemplate.opsForValue().get(key);
            } else {
                value = localCache.get(key);
            }
            
            if (value == null) {
                return Optional.empty();
            }

            if (clazz.isInstance(value)) {
                return Optional.of(clazz.cast(value));
            }

            log.warn("Value type mismatch. Expected: {}, Actual: {}", 
                    clazz.getSimpleName(), value.getClass().getSimpleName());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving value from cache: {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void put(CacheKeyType cacheKeyType, Object value, String... keyArguments) {
        put(cacheKeyType, value, cacheKeyType.getTtlSeconds(), keyArguments);
    }

    @Override
    public void put(CacheKeyType cacheKeyType, Object value, long ttlSeconds, String... keyArguments) {
        try {
            String key = acquireKey(cacheKeyType, keyArguments);
            
            if (redisAvailable) {
                if (ttlSeconds > 0) {
                    redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
                } else {
                    redisTemplate.opsForValue().set(key, value);
                }
                log.debug("Redis cache stored: key={}, ttl={}s", key, ttlSeconds);
            } else {
                // Local cache với cleanup đơn giản (không có TTL)
                localCache.put(key, value);
                log.debug("Local cache stored: key={}", key);
            }
            
        } catch (Exception e) {
            log.error("Error storing value in cache: {}", e.getMessage());
            // KHÔNG throw exception, chỉ log error
            // throw new RuntimeException("Failed to store value in cache", e);
        }
    }

    @Override
    public void delete(CacheKeyType cacheKeyType, String... keyArguments) {
        try {
            String key = acquireKey(cacheKeyType, keyArguments);
            
            if (redisAvailable) {
                redisTemplate.delete(key);
                log.debug("Redis cache deleted: key={}", key);
            } else {
                localCache.remove(key);
                log.debug("Local cache deleted: key={}", key);
            }
        } catch (Exception e) {
            log.error("Error deleting key from cache: {}", e.getMessage());
        }
    }

    private String acquireKey(CacheKeyType cacheKeyType, String... keyArguments) {
        try {
            if (keyArguments == null || keyArguments.length == 0) {
                return cacheKeyType.getKeyPattern();
            }
            
            return String.format(cacheKeyType.getKeyPattern(), (Object[]) keyArguments);
        } catch (Exception e) {
            log.error("Error generating cache key: {}", e.getMessage());
            return "cache:" + cacheKeyType.name() + ":" + 
                   (keyArguments != null ? String.join(":", keyArguments) : "default");
        }
    }
}