package com.zhs.common.cachekey.member;

import com.zhs.common.cachekey.MiniCacheKey;

import java.util.concurrent.TimeUnit;

/**
 * 会员密码鉴权版本（修改密码后递增，用于使旧 token 失效）
 * key：member:auth:pwdVer:{memberId}
 */
public class MemberPasswordAuthVersionCacheKey implements MiniCacheKey {

    private static final String PREFIX = "member:auth:pwdVer:";

    private final Long memberId;

    public MemberPasswordAuthVersionCacheKey(Long memberId) {
        this.memberId = memberId;
    }

    @Override
    public String buildKey() {
        return PREFIX + memberId;
    }

    @Override
    public long getExpireTime() {
        return 365;
    }

    @Override
    public TimeUnit getExpireTimeUnit() {
        return TimeUnit.DAYS;
    }
}
