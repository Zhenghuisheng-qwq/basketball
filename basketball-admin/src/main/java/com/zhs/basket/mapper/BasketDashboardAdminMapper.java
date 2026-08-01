package com.zhs.basket.mapper;

import com.zhs.basket.vo.DashboardDailyCountVo;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
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

    List<DashboardDailyCountVo> countPlayerCertDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countTeamAuditDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countTeamRegistrationDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countRosterChangeDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countPlatformChangeDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countMatchDataSubmissionDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countMatchDisputeDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    List<DashboardDailyCountVo> countMatchExceptionDaily(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countPlayerCertNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countTeamAuditNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countTeamRegistrationNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countRosterChangeNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countPlatformChangeNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchDataSubmissionNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchDisputeNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchExceptionNewBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countPlayerCertHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countTeamAuditHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countTeamRegistrationHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countRosterChangeHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countPlatformChangeHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchDataHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchDisputeHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    int countMatchExceptionHandledBetween(@Param("startTime") Date startTime, @Param("endTime") Date endTime);
}
