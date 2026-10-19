package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.vo.v4.BasketV4MatchListVo;
import com.zhs.common.core.redis.RedisCache;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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
    private static final String GEN_PREFIX = PREFIX + "gen:";
    /**
     * 代次递增和数据键删除必须同一条 Lua。拆开后，代次已增加但旧值还在的窗口里，
     * 公开读会直接命中旧缓存并返回。
     */
    private static final DefaultRedisScript<Long> BUMP_AND_DELETE = bumpAndDeleteScript();

    @Resource
    private RedisCache redisCache;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private BasketDivisionStageMapper divisionStageMapper;
    @Resource
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Resource
    private BasketV4MatchMapper matchMapper;

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
        if (event != null) {
            addCurrentDivisionDisplays(keys, event.getDivisionId());
            addRetiredDisplays(keys, event);
        }
        if (keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            if (key == null || !key.startsWith(PREFIX) || key.startsWith(GEN_PREFIX)) {
                continue;
            }
            try {
                stringRedisTemplate.execute(
                        BUMP_AND_DELETE,
                        java.util.List.of(GEN_PREFIX + key.substring(PREFIX.length()), key));
            } catch (Exception ex) {
                log.warn("public cache generation bump failed, key={}: {}", key, ex.getMessage());
            }
        }
    }

    private static DefaultRedisScript<Long> bumpAndDeleteScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                redis.call('INCR', KEYS[1])
                redis.call('DEL', KEYS[2])
                return 1
                """);
        return script;
    }

    /**
     * 资料或名单变更后，比赛详情、系列赛和阶段榜不在 division 通配键里。按本赛区主键逐个失效。
     */
    private void addCurrentDivisionDisplays(Set<String> keys, Long divisionId) {
        if (divisionId == null) {
            return;
        }
        try {
            java.util.List<BasketDivisionStage> stages = divisionStageMapper.selectByDivisionId(divisionId);
            if (stages != null) {
                for (BasketDivisionStage stage : stages) {
                    if (stage == null || stage.getId() == null) {
                        continue;
                    }
                    keys.add(PREFIX + "v4:stage:standings:" + stage.getId());
                    keys.add(PREFIX + "v4:stage:bracket:" + stage.getId());
                    keys.add(PREFIX + "v4:stage:series:" + stage.getId());
                }
            }
            java.util.List<BasketMatchSeries> seriesList = matchSeriesMapper.selectByDivisionId(divisionId);
            if (seriesList != null) {
                for (BasketMatchSeries series : seriesList) {
                    if (series != null && series.getId() != null) {
                        keys.add(PREFIX + "v4:match-series:" + series.getId());
                    }
                }
            }
            BasketV4MatchQuery query = new BasketV4MatchQuery();
            query.setDivisionId(divisionId);
            java.util.List<BasketV4MatchListVo> matches = matchMapper.selectVoList(query);
            if (matches != null) {
                for (BasketV4MatchListVo match : matches) {
                    if (match != null && match.getId() != null) {
                        keys.add(PREFIX + "v4:match:" + match.getId());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("public cache division display keys skipped, divisionId={}: {}", divisionId, ex.getMessage());
        }
    }

    private void addRetiredDisplays(Set<String> keys, V4PublicReadCacheEvictEvent event) {
        addIdKeys(keys, event.getRetiredMatchIds(), "v4:match:");
        addIdKeys(keys, event.getRetiredSeriesIds(), "v4:match-series:");
        addIdKeys(keys, event.getRetiredStageIds(), "v4:stage:bracket:");
        addIdKeys(keys, event.getRetiredStageIds(), "v4:stage:series:");
        addIdKeys(keys, event.getRetiredStageIds(), "v4:stage:standings:");
    }

    private void addIdKeys(Set<String> keys, java.util.List<Long> ids, String suffix) {
        if (ids == null) {
            return;
        }
        for (Long id : ids) {
            if (id != null) {
                keys.add(PREFIX + suffix + id);
            }
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