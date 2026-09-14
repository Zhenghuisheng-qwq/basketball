package com.zhs.basket.support.v4;

import com.zhs.common.core.redis.RedisCache;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class V4PublicReadCacheEvictorTest {

    @Mock
    private RedisCache redisCache;
    @InjectMocks
    private V4PublicReadCacheEvictor evictor;

    @Test
    void listenerIsAfterCommit() throws Exception {
        Method method = V4PublicReadCacheEvictor.class.getMethod(
                "onOfficialResultCommitted", V4PublicReadCacheEvictEvent.class);
        TransactionalEventListener ann = method.getAnnotation(TransactionalEventListener.class);
        assertEquals(TransactionPhase.AFTER_COMMIT, ann.phase());
        assertTrue(ann.fallbackExecution());
    }

    @Test
    void matchMutationCollectsDependencyKeys() {
        when(redisCache.keys(anyString())).thenReturn(Collections.emptySet());
        V4PublicReadCacheEvictEvent event = new V4PublicReadCacheEvictEvent(
                177L, 100L, 142L, 9L, 11L, 22L, 33L);
        Set<String> keys = evictor.collectKeys(event);
        assertTrue(keys.contains("basket:public:v4:match:100"));
        assertTrue(keys.contains("basket:public:v4:standing:177"));
        assertTrue(keys.contains("basket:public:v4:schedule:177"));
        assertTrue(keys.contains("basket:public:v4:match-series:33"));
        assertTrue(keys.contains("basket:public:v4:season:142"));
        assertTrue(keys.contains("basket:public:home:aggregate"));
        assertFalse(keys.contains("basket:public:v4:series:list"));
    }

    @Test
    void evictDeletesCollectedKeys() {
        when(redisCache.keys(anyString())).thenReturn(Collections.emptySet());
        V4PublicReadCacheEvictEvent event = new V4PublicReadCacheEvictEvent(
                177L, 100L, 142L, 9L, 11L, 22L, 33L);
        evictor.evict(event);
        verify(redisCache).deleteObject(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void rollbackPathDoesNotCallEvictWhenListenerNotInvoked() {
        verify(redisCache, never()).deleteObject(org.mockito.ArgumentMatchers.anyCollection());
        verify(redisCache, never()).deleteObject(org.mockito.ArgumentMatchers.anyString());
    }
}