package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码重发频控
 * key：member:email:code:limit:{purpose}:{email}
 */
public class EmailCodeLimitCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:email:code:limit:";

    private final String purpose;
    private final String email;
    private final long expireSeconds;

    public EmailCodeLimitCacheKey(String purpose, String email, long expireSeconds) {
        this.purpose = purpose;
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : 60;
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
