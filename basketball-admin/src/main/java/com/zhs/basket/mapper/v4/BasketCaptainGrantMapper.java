package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.vo.AccountPlayerCaptainGrantVo;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface BasketCaptainGrantMapper {
    BasketCaptainGrant selectById(@Param("id") Long id);

    List<BasketCaptainGrant> selectList(BasketCaptainGrant query);

    List<BasketCaptainGrantListVo> selectVoList(BasketCaptainGrantQuery query);

    BasketCaptainGrant selectActiveByDivisionTeam(@Param("divisionId") Long divisionId, @Param("teamId") Long teamId);

    BasketCaptainGrant selectActiveByDivisionPlayer(@Param("divisionId") Long divisionId, @Param("playerId") Long playerId);

    int expireById(@Param("id") Long id);

    List<BasketCaptainGrant> selectActiveByDivisionId(@Param("divisionId") Long divisionId);

    /**
     * Division Closing：ACTIVE → EXPIRED，释放 active_unique_key，并截断未到期的 valid_to。
     */
    int expireActiveByDivisionId(@Param("divisionId") Long divisionId, @Param("finishTime") java.util.Date finishTime);

    int bindTeamById(@Param("id") Long id, @Param("teamId") Long teamId);

    BasketCaptainGrant selectLatestActiveByMemberId(@Param("memberId") Long memberId);

    BasketCaptainGrant selectLatestActiveByPlayerId(@Param("playerId") Long playerId);

    int countRuntimeActiveByPlayerId(@Param("playerId") Long playerId);

    List<AccountPlayerCaptainGrantVo> selectGrantHistoryByMemberId(@Param("memberId") Long memberId);

    List<BasketCaptainGrant> selectBySourceDrawTeamId(@Param("sourceDrawTeamId") Long sourceDrawTeamId);

    int revokeBySourceDrawTeamId(@Param("sourceDrawTeamId") Long sourceDrawTeamId);

    int insert(BasketCaptainGrant entity);

    int updateById(BasketCaptainGrant entity);

    int updateValidWindow(@Param("id") Long id,
                          @Param("validTo") Date validTo,
                          @Param("validToMode") String validToMode,
                          @Param("expectedValidTo") Date expectedValidTo,
                          @Param("expectedValidToMode") String expectedValidToMode);

    List<BasketCaptainGrant> selectAutoEventByDivisionId(@Param("divisionId") Long divisionId);

    List<BasketCaptainGrant> selectAutoEventDependingOnSeason(@Param("seasonId") Long seasonId);

    int revokeById(@Param("id") Long id);

    int softDelete(@Param("id") Long id);
}
