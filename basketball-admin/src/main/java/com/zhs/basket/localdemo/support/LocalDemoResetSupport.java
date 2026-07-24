package com.zhs.basket.localdemo.support;



import com.zhs.basket.localdemo.constant.BasketLocalDemoConstants;

import com.zhs.basket.localdemo.mapper.BasketLocalDemoDataMapper;

import com.zhs.basket.localdemo.resp.BasketLocalDemoResetResp;

import org.springframework.stereotype.Component;



import java.util.ArrayList;

import java.util.Collections;

import java.util.List;



@Component

public class LocalDemoResetSupport {



    private final BasketLocalDemoDataMapper demoMapper;



    public LocalDemoResetSupport(BasketLocalDemoDataMapper demoMapper) {

        this.demoMapper = demoMapper;

    }



    /**

     * 按依赖关系倒序清理 LOCAL-DEMO- 标识数据，禁止影响手工测试数据。

     */

    public BasketLocalDemoResetResp reset(String marker) {

        long started = System.currentTimeMillis();

        BasketLocalDemoResetResp resp = new BasketLocalDemoResetResp();

        resp.setMarker(marker);

        resp.setStartedTime(started);



        String prefix = BasketLocalDemoConstants.BIZ_PREFIX;

        if (demoMapper.existsNonDemoReference(prefix) > 0) {

            resp.getWarnings().add("检测到演示数据被非演示业务引用，部分记录可能无法安全清理");

        }



        Long seasonId = demoMapper.selectSeasonIdBySeasonNoForReset(BasketLocalDemoConstants.SEASON_NO);

        Long groupId = demoMapper.selectGroupIdByGroupCodeForReset(BasketLocalDemoConstants.GROUP_CODE);

        List<Long> matchIds = demoMapper.selectMatchIdsByPrefix(prefix);

        List<Long> registrationIds = demoMapper.selectRegistrationIdsByPrefix(prefix);

        List<Long> teamIds = demoMapper.selectTeamIdsByPrefix(prefix);

        List<Long> applicationIds = demoMapper.selectApplicationIdsByPrefix(prefix);

        List<Long> accountIds = demoMapper.selectAccountIdsByPrefix(prefix);



        int deleted = 0;

        deleted += demoMapper.deleteBannersByPrefix(prefix);

        deleted += demoMapper.deleteAnnouncementsByPrefix(prefix);

        if (seasonId != null) {

            deleted += demoMapper.deletePlayerRanksByDemoSeason(seasonId);

            deleted += demoMapper.deleteTeamRanksByDemoSeason(seasonId);

            deleted += demoMapper.deletePlayerSeasonStatsBySeasonId(seasonId);

            deleted += demoMapper.deleteTeamSeasonStatsBySeasonId(seasonId);

            deleted += demoMapper.deleteStandingsBySeasonId(seasonId);

            deleted += demoMapper.deleteTournamentRulesBySeasonId(seasonId, marker);

            deleted += demoMapper.deleteTeamSeasonProfilesBySeasonId(seasonId, prefix);

        }

        if (!matchIds.isEmpty()) {

            deleted += demoMapper.deletePlayerMatchStatsByMatchIds(matchIds);

            deleted += demoMapper.deleteTeamMatchStatsByMatchIds(matchIds);

            deleted += demoMapper.deletePeriodScoresByMatchIds(matchIds);

            deleted += demoMapper.deleteLineupsByMatchIds(matchIds);

            deleted += demoMapper.deleteMatchTeamsByMatchIds(matchIds);

            deleted += demoMapper.deleteMatchStatusLogsByMatchIds(matchIds);

        }

        deleted += demoMapper.deleteMatchesByPrefix(prefix);

        if (groupId != null) {

            deleted += demoMapper.deleteGroupTeamsByGroupId(groupId);

        }

        deleted += demoMapper.deleteGroupsByPrefix(prefix);

        deleted += demoMapper.deleteRoundsByPrefix(prefix);

        deleted += demoMapper.deleteStagesByPrefix(prefix);

        if (!registrationIds.isEmpty()) {

            deleted += demoMapper.deleteRegistrationAuditsByRegistrationIds(registrationIds);

            deleted += demoMapper.deleteRegistrationPlayersByRegistrationIds(registrationIds);

        }

        deleted += demoMapper.deleteRegistrationsByPrefix(prefix);

        if (!teamIds.isEmpty()) {

            deleted += demoMapper.deleteTeamMembersByTeamIds(teamIds);

            deleted += demoMapper.deleteTeamInvitationsByTeamIds(teamIds);

            deleted += demoMapper.deleteTeamAuditsByTeamIds(teamIds);

        }

        deleted += demoMapper.deleteTeamMembersByPrefix(prefix);

        deleted += demoMapper.deleteTeamInvitationsByPrefix(prefix);

        deleted += demoMapper.deleteTeamsByPrefix(prefix);

        deleted += demoMapper.deleteCaptainQualificationsByPrefix(prefix, marker);

        if (!applicationIds.isEmpty()) {

            deleted += demoMapper.deleteCertAuditsByApplicationIds(applicationIds);

            deleted += demoMapper.deleteCertMaterialsByApplicationIds(applicationIds);

        }

        deleted += demoMapper.deleteCertApplicationsByPrefix(prefix);

        deleted += demoMapper.deletePlayersByPrefix(prefix);

        if (!accountIds.isEmpty()) {

            deleted += demoMapper.deleteMemberProfilesByAccountIds(accountIds);

            deleted += demoMapper.deletePrivacyByMemberIds(accountIds);

            deleted += demoMapper.deleteNotificationByMemberIds(accountIds);

        }

        deleted += demoMapper.deleteAccountsByPrefix(prefix);

        deleted += demoMapper.deleteSeasonsByPrefix(prefix);

        deleted += demoMapper.deleteTournamentsByPrefix(prefix);

        deleted += demoMapper.deletePlaceholderFilesByRemark(marker);



        resp.setDeletedTotal(deleted);

        List<String> summary = new ArrayList<>();

        summary.add("演示数据物理删除完成，合计 " + deleted + " 条");

        resp.setDeletedSummary(summary);

        resp.setFullyCleared(demoMapper.countAccountsIncludingDeleted(prefix) == 0);

        resp.setMessage(resp.isFullyCleared() ? "演示数据已完全清理" : "部分演示数据未能清理，请检查数据库");

        resp.setFinishedTime(System.currentTimeMillis());

        resp.setDurationMillis(resp.getFinishedTime() - started);

        return resp;

    }

}


