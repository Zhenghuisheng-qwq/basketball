package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * Email verification code stored in Redis.
 * key: member:email:code:{purpose}:{email}
 * TTL comes from member.auth.code-expire-seconds.
 */
public class EmailCodeValueCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:email:code:";

    private final String purpose;
    private final String email;
    private final long expireSeconds;

    public EmailCodeValueCacheKey(String purpose, String email, long expireSeconds) {
        this.purpose = purpose == null ? "" : purpose.trim().toUpperCase();
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : 300;
    }

    @Override
    public String buildKey() {
        return PREFIX + purpose + ":" + email;
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
