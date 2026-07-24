package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 验证码错误次数
 * key：member:email:code:fail:{bizNo}
 */
public class EmailCodeFailCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:email:code:fail:";

    private final String bizNo;
    private final long expireSeconds;

    public EmailCodeFailCacheKey(String bizNo, long expireSeconds) {
        this.bizNo = bizNo;
        this.expireSeconds = expireSeconds > 0 ? expireSeconds : 300;
    }

    @Override
    public String buildKey() {
        return PREFIX + bizNo;
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
