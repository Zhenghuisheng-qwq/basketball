package com.zhs.basket.support.v4;

import com.zhs.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class V4PublicReadCacheEvictor {

    private static final Logger log = LoggerFactory.getLogger(V4PublicReadCacheEvictor.class);
    private static final String PREFIX = "basket:public:";

    @Resource
    private RedisCache redisCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOfficialResultCommitted(V4PublicReadCacheEvictEvent event) {
        if (event == null) {
            return;
        }
        try {
            evict(event);
        } catch (Exception ex) {
            log.warn("evict public read cache ignored: {}", ex.getMessage());
        }
    }

    public void evict(V4PublicReadCacheEvictEvent event) {
        Set<String> keys = collectKeys(event);
        if (!keys.isEmpty()) {
            redisCache.deleteObject(keys);
        }
    }

    public Set<String> collectKeys(V4PublicReadCacheEvictEvent event) {
        Set<String> keys = new LinkedHashSet<>();
        if (event == null) {
            return keys;
        }
        if (event.getMatchId() != null) {
            keys.add(PREFIX + "v4:match:" + event.getMatchId());
        }
        if (event.getMatchSeriesId() != null) {
            keys.add(PREFIX + "v4:match-series:" + event.getMatchSeriesId());
        }
        if (event.getDivisionId() != null) {
            keys.add(PREFIX + "v4:division:" + event.getDivisionId());
            keys.add(PREFIX + "v4:standing:" + event.getDivisionId());
            keys.add(PREFIX + "v4:team-stat:" + event.getDivisionId());
            keys.add(PREFIX + "v4:player-stat:" + event.getDivisionId());
            keys.add(PREFIX + "v4:team-list:" + event.getDivisionId());
            keys.add(PREFIX + "v4:schedule:" + event.getDivisionId());
            addKeys(keys, PREFIX + "v4:match-list:" + event.getDivisionId() + "*");
            addKeys(keys, PREFIX + "v4:rank:" + event.getDivisionId() + "*");
            addKeys(keys, PREFIX + "v4:stage:list:" + event.getDivisionId() + "*");
        }
        if (event.getSeasonId() != null) {
            keys.add(PREFIX + "v4:season:" + event.getSeasonId());
            keys.add(PREFIX + "v4:division-list:" + event.getSeasonId());
            addKeys(keys, PREFIX + "v4:season:list:*");
        }
        if (event.getStageId() != null) {
            addKeys(keys, PREFIX + "v4:stage:standings:" + event.getStageId() + "*");
            addKeys(keys, PREFIX + "v4:stage:bracket:" + event.getStageId() + "*");
            addKeys(keys, PREFIX + "v4:stage:series:" + event.getStageId() + "*");
        }
        if (event.getHomeEventTeamId() != null) {
            keys.add(PREFIX + "v4:team:" + event.getHomeEventTeamId());
        }
        if (event.getAwayEventTeamId() != null) {
            keys.add(PREFIX + "v4:team:" + event.getAwayEventTeamId());
        }
        if (event.getPlayerId() != null) {
            keys.add(PREFIX + "player:detail:" + event.getPlayerId());
            addKeys(keys, PREFIX + "player:season-stat:" + event.getPlayerId() + "*");
        }
        if (event.isEvictSeriesList()) {
            keys.add(PREFIX + "v4:series:list");
        }
        boolean resultChanged = event.getMatchId() != null
                || event.getDivisionId() != null
                || event.getStageId() != null
                || event.getMatchSeriesId() != null;
        if (resultChanged) {
            keys.add(PREFIX + "home:aggregate");
            addKeys(keys, PREFIX + "player:season-stat:*");
        } else if (event.getSeasonId() != null || event.isEvictSeriesList()) {
            keys.add(PREFIX + "home:aggregate");
        }
        return keys;
    }

    private void addKeys(Set<String> target, String pattern) {
        Collection<String> found = redisCache.keys(pattern);
        if (!CollectionUtils.isEmpty(found)) {
            target.addAll(found);
        }
    }
}