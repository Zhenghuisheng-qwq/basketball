package com.zhs.common.cachekey.publicapi;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 官网公开查询短缓存。
 * key 前缀：basket:public:
 */
public class BasketPublicCacheKey implements MiniCacheKey {

    public static final String PREFIX = "basket:public:";
    public static final long DEFAULT_SECONDS = 60L;

    private final String suffix;
    private final long expireSeconds;

    public BasketPublicCacheKey(String suffix) {
        this(suffix, DEFAULT_SECONDS);
    }

    public BasketPublicCacheKey(String suffix, long expireSeconds) {
        this.suffix = suffix == null ? "" : suffix;
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : DEFAULT_SECONDS;
    }

    @Override
    public String buildKey() {
        return PREFIX + suffix;
    }

    @Override
    public long getExpireTime() {
        return expireSeconds;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.SECONDS;
    }
}
