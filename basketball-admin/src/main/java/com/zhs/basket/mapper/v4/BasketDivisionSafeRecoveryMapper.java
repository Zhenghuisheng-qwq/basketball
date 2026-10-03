package com.zhs.basket.mapper.v4;

import org.apache.ibatis.annotations.Param;

/**
 * Division 安全删除 / 结束赛事的只读计数，以及不改动比赛核心写链路。
 */
public interface BasketDivisionSafeRecoveryMapper {

    int countTeamRegistration(@Param("divisionId") Long divisionId);

    int countPersonalRegistration(@Param("divisionId") Long divisionId);

    int countRoster(@Param("divisionId") Long divisionId);

    int countEventTeam(@Param("divisionId") Long divisionId);

    int countEventPlayer(@Param("divisionId") Long divisionId);

    int countCaptainGrant(@Param("divisionId") Long divisionId);

    int countScheduleBatch(@Param("divisionId") Long divisionId);

    int countMatchSeries(@Param("divisionId") Long divisionId);

    int countMatch(@Param("divisionId") Long divisionId);

    int countStanding(@Param("divisionId") Long divisionId);

    int countStats(@Param("divisionId") Long divisionId);

    int countSubmission(@Param("divisionId") Long divisionId);

    int countQualification(@Param("divisionId") Long divisionId);

    /**
     * 已发布且比赛状态为正式完成结果的 Match 数量。
     * 终态取自 V4 MatchStatusEnum 与正式结算口径：FINISHED、FORFEITED。
     */
    int countCompletedOfficialMatches(@Param("divisionId") Long divisionId);
}
