package com.zhs.basket.support;

import com.zhs.common.core.redis.RedisCache;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 赛季重算分布式锁：basket:data:settlement:season:{seasonId}
 */
@Component
public class SettlementLockHelper {

    public static final String LOCK_KEY_PREFIX = "basket:data:settlement:season:";
    private static final int DEFAULT_EXPIRE_MINUTES = 30;

    @Autowired
    private RedisCache redisCache;

    public String tryLock(Long seasonId) {
        if (seasonId == null) {
            throw new ServiceException("赛季ID不能为空");
        }
        String key = LOCK_KEY_PREFIX + seasonId;
        String token = UUID.randomUUID().toString().replace("-", "");
        RedisTemplate redisTemplate = redisCache.redisTemplate;
        Boolean ok = redisTemplate.opsForValue().setIfAbsent(key, token, DEFAULT_EXPIRE_MINUTES, TimeUnit.MINUTES);
        if (!Boolean.TRUE.equals(ok)) {
            throw new ServiceException("当前赛季数据正在重算，请稍后重试");
        }
        return token;
    }

    public void unlock(Long seasonId, String token) {
        if (seasonId == null || token == null) {
            return;
        }
        String key = LOCK_KEY_PREFIX + seasonId;
        try {
            Object current = redisCache.getCacheObject(key);
            if (current != null && token.equals(String.valueOf(current))) {
                redisCache.deleteObject(key);
            }
        } catch (Exception ignored) {
            // 锁释放失败不影响主流程；依赖 TTL 过期
        }
    }
}
