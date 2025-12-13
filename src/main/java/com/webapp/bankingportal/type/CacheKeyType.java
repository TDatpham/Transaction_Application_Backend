/*
package com.webapp.bankingportal.type;

public enum CacheKeyType {
    IDEMPOTENCY("IDPT:%s:%s:%s", 86400, CacheValueType.JSON),
    ;

    private final String prefix;
    private final long ttlSeconds;
    private final CacheValueType cacheValueType;

    CacheKeyType(String prefix, long ttlSeconds, CacheValueType cacheValueType) {
        this.prefix = prefix;
        this.ttlSeconds = ttlSeconds;
        this.cacheValueType = cacheValueType;
    }

    public String getPrefix() {
        return prefix;
    }

    public long getTtlSeconds() {
        return ttlSeconds;
    }

    public CacheValueType getCacheValueType() {
        return cacheValueType;
    }

    public String generateKey() {
        return prefix;
    }

    public String generateKey(String... arguments) {
        return String.format(prefix, (Object[]) arguments);
    }
}
*/
package com.webapp.bankingportal.type;

public enum CacheKeyType {
    
    // SỬA: Đảm bảo số lượng %s khớp với số arguments
    IDEMPOTENCY("idempotency:%s", 300), // 1 argument
    
    // Các cache key khác
    USER("user:%s", 3600),              // 1 argument
    ACCOUNT("account:%s", 1800),        // 1 argument
    OTP("otp:%s:%s", 600),              // 2 arguments
    LOGIN_ATTEMPT("login:attempt:%s", 900); // 1 argument
    
    private final String keyPattern;
    private final long ttlSeconds;
    
    CacheKeyType(String keyPattern, long ttlSeconds) {
        this.keyPattern = keyPattern;
        this.ttlSeconds = ttlSeconds;
    }
    
    public String getKeyPattern() {
        return keyPattern;
    }
    
    public long getTtlSeconds() {
        return ttlSeconds;
    }
    
    public String generateKey(String... arguments) {
        return String.format(keyPattern, (Object[]) arguments);
    }
}