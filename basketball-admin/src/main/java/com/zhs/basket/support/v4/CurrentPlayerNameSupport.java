package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.vo.EligibleCaptainVo;
import com.zhs.basket.vo.v4.BasketMatchSubmissionPlayerStatVo;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 审核收集页跟球队总览同一套昵称。会员昵称改了，下次打开详情就是新名字。
 */
@Component
public class CurrentPlayerNameSupport {

    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;

    public void overlaySubmissionNames(List<BasketMatchSubmissionPlayerStatVo> stats, Map<Long, Long> eventPlayerToSource) {
        if (CollectionUtils.isEmpty(stats) || CollectionUtils.isEmpty(eventPlayerToSource)) {
            return;
        }
        Map<Long, String> names = bySourcePlayerId(eventPlayerToSource.values());
        for (BasketMatchSubmissionPlayerStatVo stat : stats) {
            if (stat == null || stat.getEventPlayerId() == null) {
                continue;
            }
            Long sourceId = eventPlayerToSource.get(stat.getEventPlayerId());
            String name = sourceId == null ? null : names.get(sourceId);
            if (StringUtils.isNotEmpty(name)) {
                stat.setPlayerName(name);
            }
        }
    }

    /** 账户昵称优先，没有昵称再用认证时的游戏昵称。 */
    public String displayName(Long playerId) {
        if (playerId == null) {
            return null;
        }
        return bySourcePlayerId(List.of(playerId)).get(playerId);
    }

    public void rememberSource(Map<Long, Long> eventPlayerToSource, BasketEventPlayer eventPlayer) {
        if (eventPlayer == null || eventPlayer.getId() == null || eventPlayer.getSourcePlayerId() == null) {
            return;
        }
        eventPlayerToSource.put(eventPlayer.getId(), eventPlayer.getSourcePlayerId());
    }

    private Map<Long, String> bySourcePlayerId(Collection<Long> sourcePlayerIds) {
        Map<Long, String> result = new HashMap<>();
        if (CollectionUtils.isEmpty(sourcePlayerIds)) {
            return result;
        }
        Set<Long> ids = new HashSet<>();
        for (Long id : sourcePlayerIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return result;
        }
        List<EligibleCaptainVo> rows = playerProfileMapper.selectCurrentNicknames(List.copyOf(ids));
        if (rows == null) {
            return result;
        }
        for (EligibleCaptainVo row : rows) {
            if (row == null || row.getPlayerId() == null) {
                continue;
            }
            String name = StringUtils.isNotEmpty(row.getNickname()) ? row.getNickname().trim()
                    : (StringUtils.isNotEmpty(row.getGameNickname()) ? row.getGameNickname().trim() : null);
            if (StringUtils.isNotEmpty(name)) {
                result.put(row.getPlayerId(), name);
            }
        }
        return result;
    }
}
