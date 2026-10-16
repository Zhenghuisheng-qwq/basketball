package com.zhs.basket.mapper.v4;

import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.vo.v4.BasketRegistrationPlayerVo;
import com.zhs.basket.vo.v4.RegistrationGapMemberVo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketRegistrationPlayerMapper {
    BasketRegistrationPlayer selectById(@Param("id") Long id);

    List<BasketRegistrationPlayer> selectList(BasketRegistrationPlayer query);

    List<BasketRegistrationPlayerVo> selectVoByRegistrationId(@Param("registrationId") Long registrationId);

    List<RegistrationGapMemberVo> selectCurrentMembersOutsideRoster(@Param("registrationId") Long registrationId,
                                                                     @Param("teamId") Long teamId);

    List<BasketRegistrationPlayer> selectByRegistrationId(@Param("registrationId") Long registrationId);

    int deleteByRegistrationId(@Param("registrationId") Long registrationId);

    int insert(BasketRegistrationPlayer entity);

    int batchInsert(@Param("list") List<BasketRegistrationPlayer> list);

    int updateById(BasketRegistrationPlayer entity);

    int softDelete(@Param("id") Long id);

    int countCurrentEligible(@Param("registrationId") Long registrationId);

    int countRosterPlayers(@Param("registrationId") Long registrationId);

    int countActiveMember(@Param("teamId") Long teamId, @Param("playerId") Long playerId);

    int countActivePlayerInDivision(@Param("divisionId") Long divisionId,
                                    @Param("playerId") Long playerId,
                                    @Param("excludeRegistrationId") Long excludeRegistrationId);
}
