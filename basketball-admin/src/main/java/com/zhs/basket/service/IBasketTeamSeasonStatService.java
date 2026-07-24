package com.zhs.basket.service;

import com.zhs.basket.query.TeamSeasonStatQuery;
import com.zhs.basket.vo.TeamSeasonStatListVo;

import java.util.Date;
import java.util.List;

/**
 * 球队赛季统计汇总。
 */
public interface IBasketTeamSeasonStatService {

    List<TeamSeasonStatListVo> selectList(TeamSeasonStatQuery query);

    /**
     * 重算赛季球队统计；stageId=0 为赛季汇总，并按阶段生成 stage 维度汇总。
     */
    int rebuildSeason(Long seasonId, Date calcTime);
}
