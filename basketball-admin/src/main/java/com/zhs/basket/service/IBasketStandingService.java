package com.zhs.basket.service;

import com.zhs.basket.query.StandingQuery;
import com.zhs.basket.support.StandingPointsRule;
import com.zhs.basket.vo.StandingListVo;

import java.util.Date;
import java.util.List;

/**
 * 阶段/小组积分榜。
 */
public interface IBasketStandingService {

    List<StandingListVo> selectList(StandingQuery query);

    int rebuildSeasonStandings(Long seasonId, StandingPointsRule rule, Date snapshotTime);

    int rebuildStageStandings(Long stageId, StandingPointsRule rule, Date snapshotTime);

    int rebuildGroupStandings(Long groupId, StandingPointsRule rule, Date snapshotTime);
}
