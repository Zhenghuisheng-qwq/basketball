package com.zhs.basket.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketTournamentAdminScopeMapper {
    int countAuthorized(@Param("adminUserId") Long adminUserId, @Param("tournamentId") Long tournamentId);

    List<Long> selectTournamentIdsByAdmin(@Param("adminUserId") Long adminUserId);

    List<com.zhs.basket.vo.TournamentScopeListVo> selectList(@Param("tournamentId") Long tournamentId,
            @Param("adminUserId") Long adminUserId);

    int countActiveGrant(@Param("adminUserId") Long adminUserId, @Param("tournamentId") Long tournamentId,
            @Param("scopeCode") String scopeCode);

    int insert(com.zhs.basket.domain.BasketTournamentAdminScope row);

    int deactivateById(@Param("id") Long id);
}
