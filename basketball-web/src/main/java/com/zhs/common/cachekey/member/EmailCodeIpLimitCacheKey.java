package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 邮箱验证码 IP 小时限额
 * key：member:email:code:ip:{yyyyMMddHH}:{ip}
 */
public class EmailCodeIpLimitCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:email:code:ip:";

    private final String hourBucket;
    private final String ip;

    public EmailCodeIpLimitCacheKey(String hourBucket, String ip) {
        this.hourBucket = hourBucket;
        this.ip = ip == null ? "unknown" : ip.trim();
    }

    @Override
    public String buildKey() {
        return PREFIX + hourBucket + ":" + ip;
    }

    @Override
    public long getExpireTime() {
        return 2;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.HOURS;
    }
}
