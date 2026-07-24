package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码日限额
 * key：member:email:code:day:{yyyyMMdd}:{email}
 */
public class EmailCodeDailyLimitCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:email:code:day:";

    private final String day;
    private final String email;

    public EmailCodeDailyLimitCacheKey(String day, String email) {
        this.day = day;
        this.email = email == null ? "" : email.trim().toLowerCase();
    }

    @Override
    public String buildKey() {
        return PREFIX + day + ":" + email;
    }

    @Override
    public long getExpireTime() {
        return 2;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.DAYS;
    }
}
