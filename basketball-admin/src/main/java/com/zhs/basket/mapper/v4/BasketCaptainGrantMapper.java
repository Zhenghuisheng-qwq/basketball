package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.vo.AccountPlayerCaptainGrantVo;
import com.zhs.basket.vo.v4.BasketCaptainGrantListVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketCaptainGrantMapper {
    BasketCaptainGrant selectById(@Param("id") Long id);

    List<BasketCaptainGrant> selectList(BasketCaptainGrant query);

    List<BasketCaptainGrantListVo> selectVoList(BasketCaptainGrantQuery query);

    BasketCaptainGrant selectActiveByDivisionTeam(@Param("divisionId") Long divisionId, @Param("teamId") Long teamId);

    BasketCaptainGrant selectLatestActiveByMemberId(@Param("memberId") Long memberId);

    BasketCaptainGrant selectLatestActiveByPlayerId(@Param("playerId") Long playerId);

    int countRuntimeActiveByPlayerId(@Param("playerId") Long playerId);

    List<AccountPlayerCaptainGrantVo> selectGrantHistoryByMemberId(@Param("memberId") Long memberId);

    List<BasketCaptainGrant> selectBySourceDrawTeamId(@Param("sourceDrawTeamId") Long sourceDrawTeamId);

    int revokeBySourceDrawTeamId(@Param("sourceDrawTeamId") Long sourceDrawTeamId);

    int insert(BasketCaptainGrant entity);

    int updateById(BasketCaptainGrant entity);

    int revokeById(@Param("id") Long id);

    int softDelete(@Param("id") Long id);
}
