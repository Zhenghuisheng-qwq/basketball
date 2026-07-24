package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 密码登录失败计数
 * key：member:login:fail:{account}
 */
public class LoginFailCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:login:fail:";

    private final String account;
    private final long expireMinutes;

    public LoginFailCacheKey(String account, long expireMinutes) {
        this.account = account == null ? "" : account.trim().toLowerCase();
        this.expireMinutes = expireMinutes > 0 ? expireMinutes : 15;
    }

    @Override
    public String buildKey() {
        return PREFIX + account;
    }

    @Override
    public long getExpireTime() {
        return expireMinutes;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.MINUTES;
    }
}
