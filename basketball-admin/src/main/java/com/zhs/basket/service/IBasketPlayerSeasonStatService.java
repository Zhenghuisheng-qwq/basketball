package com.zhs.basket.service;

import com.zhs.basket.query.PlayerSeasonStatQuery;
import com.zhs.basket.vo.PlayerSeasonStatListVo;

import java.util.Date;
import java.util.List;

/**
 * 球员赛季统计汇总。
 */
public interface IBasketPlayerSeasonStatService {

    List<PlayerSeasonStatListVo> selectList(PlayerSeasonStatQuery query);

    /**
     * 重算赛季球员统计；stageId=0 为赛季汇总，并按阶段生成 stage 维度汇总。
     */
    int rebuildSeason(Long seasonId, Date calcTime);
}
