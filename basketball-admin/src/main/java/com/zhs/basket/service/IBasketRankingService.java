package com.zhs.basket.service;

import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.vo.PlayerRankListVo;
import com.zhs.basket.vo.TeamRankListVo;

import java.util.Date;
import java.util.List;

/**
 * 球队/球员排行榜。
 */
public interface IBasketRankingService {

    List<TeamRankListVo> selectTeamLatest(RankingQuery query);

    List<PlayerRankListVo> selectPlayerLatest(RankingQuery query);

    /**
     * 基于 stageId=0 的赛季统计生成全部排行维度快照。
     */
    int rebuildSeasonRankings(Long seasonId, Date snapshotDate);
}
