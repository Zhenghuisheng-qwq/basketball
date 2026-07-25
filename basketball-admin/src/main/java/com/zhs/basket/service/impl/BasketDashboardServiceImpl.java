package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.mapper.BasketDashboardAdminMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.service.IBasketDashboardService;
import com.zhs.basket.support.BasketTournamentScopeSupport;
import com.zhs.basket.vo.DashboardSummaryVo;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BasketDashboardServiceImpl implements IBasketDashboardService {

    @Autowired private BasketDashboardAdminMapper dashboardMapper;
    @Autowired private BasketTournamentScopeSupport tournamentScopeSupport;
    @Autowired private BasketTournamentMapper tournamentMapper;

    @Override
    public DashboardSummaryVo getSummary() {
        List<Long> tournamentIds = tournamentScopeSupport.accessibleTournamentIds();
        if (!SecurityUtils.isAdmin() && (tournamentIds == null || tournamentIds.isEmpty())) {
            return emptySummary();
        }
        List<Long> scopedIds = SecurityUtils.isAdmin() ? null : tournamentIds;
        DashboardSummaryVo vo = new DashboardSummaryVo();
        vo.setPendingTeamAudit(dashboardMapper.countPendingTeamAudit());
        vo.setPendingPlayerCert(dashboardMapper.countPendingPlayerCert());
        vo.setPendingRegistration(dashboardMapper.countPendingRegistration(scopedIds));
        vo.setPendingOpenRegistration(dashboardMapper.countPendingOpenRegistration(scopedIds));
        vo.setPendingRosterChange(dashboardMapper.countPendingRosterChange(scopedIds));
        vo.setPendingPlatformChange(dashboardMapper.countPendingPlatformChange());
        vo.setPendingMatchDataAudit(dashboardMapper.countPendingMatchDataAudit(scopedIds));
        vo.setPendingMatchDispute(dashboardMapper.countPendingMatchDispute(scopedIds));
        fillCurrentTournament(vo, tournamentIds);
        return vo;
    }

    private void fillCurrentTournament(DashboardSummaryVo vo, List<Long> tournamentIds) {
        Long tournamentId = null;
        if (tournamentIds != null && !tournamentIds.isEmpty()) {
            tournamentId = tournamentIds.get(0);
        }
        if (tournamentId == null && SecurityUtils.isAdmin()) {
            return;
        }
        if (tournamentId != null) {
            BasketTournament tournament = tournamentMapper.selectById(tournamentId);
            if (tournament != null) {
                vo.setCurrentTournamentId(tournament.getId());
                vo.setCurrentTournamentName(tournament.getTournamentName());
            }
        }
    }

    private DashboardSummaryVo emptySummary() {
        DashboardSummaryVo vo = new DashboardSummaryVo();
        vo.setPendingTeamAudit(0);
        vo.setPendingPlayerCert(0);
        vo.setPendingRegistration(0);
        vo.setPendingOpenRegistration(0);
        vo.setPendingRosterChange(0);
        vo.setPendingPlatformChange(0);
        vo.setPendingMatchDataAudit(0);
        vo.setPendingMatchDispute(0);
        return vo;
    }
}
