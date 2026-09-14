package com.zhs.service.support;

import com.zhs.common.cachekey.publicapi.BasketPublicCacheKey;
import com.zhs.util.MiniRedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketPublicSupportBreakerTest {

    @Mock
    private MiniRedisUtil miniRedisUtil;
    @Mock
    private ObjectMapper objectMapper;
    @InjectMocks
    private BasketPublicSupport support;

    @BeforeEach
    void initMapper() {
        when(objectMapper.copy()).thenReturn(new ObjectMapper());
        support.initCacheMapper();
    }

    @Test
    void brokenJsonDoesNotTripGlobalBreaker() {
        when(miniRedisUtil.get(any(BasketPublicCacheKey.class))).thenReturn("{not-json");
        AtomicInteger loads = new AtomicInteger();
        String first = support.cacheGetOrLoad("t:broken", () -> {
            loads.incrementAndGet();
            return "db-1";
        });
        String second = support.cacheGetOrLoad("t:other", () -> {
            loads.incrementAndGet();
            return "db-2";
        });
        assertEquals("db-1", first);
        assertEquals("db-2", second);
        assertEquals(2, loads.get());
        assertFalse(BasketPublicSupport.isRedisInfraFailure(new IllegalArgumentException("bad json")));
    }

    @Test
    void redisConnectionFailureIsInfra() {
        assertTrue(BasketPublicSupport.isRedisInfraFailure(
                new RedisConnectionFailureException("down", new RuntimeException())));
    }
}