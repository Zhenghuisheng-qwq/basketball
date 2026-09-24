package com.zhs.common.cachekey.web;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

public class WebRateLimitCacheKey implements MiniCacheKey {

    private static final String PREFIX = "web:ratelimit:";

    private final String group;
    private final String identity;
    private final long expireSeconds;

    public WebRateLimitCacheKey(String group, String identity, long expireSeconds) {
        this.group = group == null ? "UNKNOWN" : group;
        this.identity = identity == null || identity.isBlank() ? "unknown" : identity.trim();
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : 60L;
    }

    @Override
    public String buildKey() {
        return PREFIX + group + ":" + identity;
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