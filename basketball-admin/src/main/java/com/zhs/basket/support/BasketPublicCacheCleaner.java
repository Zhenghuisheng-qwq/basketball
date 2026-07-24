package com.zhs.basket.support;

import com.zhs.common.core.redis.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Collection;

/**
 * 事务提交后清理公开接口缓存 basket:public:*
 */
@Component
public class BasketPublicCacheCleaner {

    private static final Logger log = LoggerFactory.getLogger(BasketPublicCacheCleaner.class);
    private static final String PUBLIC_CACHE_PREFIX = "basket:public:*";

    @Autowired
    private RedisCache redisCache;

    public void clearAfterCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doClear();
                }
            });
            return;
        }
        doClear();
    }

    public void clearNow() {
        doClear();
    }

    private void doClear() {
        try {
            Collection<String> keys = redisCache.keys(PUBLIC_CACHE_PREFIX);
            if (keys != null && !keys.isEmpty()) {
                redisCache.deleteObject(keys);
            }
        } catch (Exception ex) {
            log.warn("清理公开缓存失败，已忽略: {}", ex.getMessage());
        }
    }
}
