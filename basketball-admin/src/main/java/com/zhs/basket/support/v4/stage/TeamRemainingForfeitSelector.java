package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * 一支球队弃赛后，只处理同一小组里尚未开赛的已发布场次。已结束、进行中、草稿和别的小组不动。
 */
public final class TeamRemainingForfeitSelector {

    private TeamRemainingForfeitSelector() {
    }

    public static List<BasketV4Match> remaining(List<BasketV4Match> stageMatches,
                                                Long anchorMatchId,
                                                Long groupId,
                                                Long loserEventTeamId) {
        List<BasketV4Match> result = new ArrayList<>();
        if (stageMatches == null || loserEventTeamId == null || groupId == null) {
            return result;
        }
        for (BasketV4Match match : stageMatches) {
            if (match == null || match.getId() == null || match.getId().equals(anchorMatchId)) {
                continue;
            }
            if (!MatchPublishStatusEnum.PUBLISHED.getCode().equals(match.getPublishStatus())) {
                continue;
            }
            if (!MatchStatusEnum.SCHEDULED.getCode().equals(match.getMatchStatus())) {
                continue;
            }
            if (!groupId.equals(match.getGroupId())) {
                continue;
            }
            boolean loserIsHome = loserEventTeamId.equals(match.getHomeEventTeamId());
            boolean loserIsAway = loserEventTeamId.equals(match.getAwayEventTeamId());
            if (!loserIsHome && !loserIsAway) {
                continue;
            }
            Long opponentId = loserIsHome ? match.getAwayEventTeamId() : match.getHomeEventTeamId();
            if (opponentId == null) {
                continue;
            }
            result.add(match);
        }
        return result;
    }
}
