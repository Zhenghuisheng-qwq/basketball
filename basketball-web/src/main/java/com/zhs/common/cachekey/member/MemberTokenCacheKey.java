package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 官网会员 Token
 * key：member:token:{token}
 */
public class MemberTokenCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:token:";

    private final String token;
    private final long expireDays;

    public MemberTokenCacheKey(String token, long expireDays) {
        this.token = token;
        this.expireDays = expireDays > 0 ? expireDays : 30;
    }

    @Override
    public String buildKey() {
        return PREFIX + token;
    }

    @Override
    public long getExpireTime() {
        return expireDays;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.DAYS;
    }
}
