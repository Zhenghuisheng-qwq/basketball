package com.zhs.common.cachekey.member;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EmailCodeValueCacheKeyTest {

    @Test
    void keyIncludesSceneAndNormalizedEmail() {
        EmailCodeValueCacheKey key = new EmailCodeValueCacheKey("register", "Test@QQ.com", 300);
        assertEquals("member:email:code:REGISTER:test@qq.com", key.buildKey());
        assertEquals(300, key.getExpireTime());
        assertEquals(TimeUnit.SECONDS, key.getExpireTimeUnit());
    }
}
