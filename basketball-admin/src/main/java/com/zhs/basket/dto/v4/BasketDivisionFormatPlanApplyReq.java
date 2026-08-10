package com.zhs.basket.dto.v4;

import lombok.Data;

/**
 * 应用赛制组合方案请求（仅业务参数，不含 snapshot/stage 内部标识）。
 */
@Data
public class BasketDivisionFormatPlanApplyReq {
    /** GROUP_TO_ELIM / ROUND_ROBIN_TO_ELIM */
    private String planCode;
    /** 预计参赛队伍数（通常取赛区队伍上限） */
    private Integer expectedTeamCount;
    /** 分组数量（GROUP_TO_ELIM） */
    private Integer groupCount;
    /** 循环次数 / 每组循环次数 */
    private Integer roundRobinLegs;
    /** 每组晋级数量（GROUP_TO_ELIM） */
    private Integer qualifiersPerGroup;
    /** 总排名前 N 晋级（ROUND_ROBIN_TO_ELIM） */
    private Integer overallAdvanceCount;
    /** 淘汰赛对阵方式：HIGH_VS_LOW / RANDOM_DRAW */
    private String eliminationPairingRule;
    private Integer winPoints;
    private Integer lossPoints;
}
