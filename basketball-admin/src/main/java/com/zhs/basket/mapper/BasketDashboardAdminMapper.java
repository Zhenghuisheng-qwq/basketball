package com.zhs.basket.mapper;

import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface BasketDashboardAdminMapper {
    int countPendingTeamAudit();

    int countPendingPlayerCert();

    int countPendingRegistration(@Param("tournamentIds") List<Long> tournamentIds);

    int countPendingOpenRegistration(@Param("tournamentIds") List<Long> tournamentIds);

    int countPendingRosterChange(@Param("tournamentIds") List<Long> tournamentIds);

    int countPendingPlatformChange();

    int countPendingMatchDataAudit(@Param("tournamentIds") List<Long> tournamentIds);

    int countPendingMatchDispute(@Param("tournamentIds") List<Long> tournamentIds);
}
