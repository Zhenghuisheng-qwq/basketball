package com.zhs.basket.localdemo.support;

import com.zhs.basket.localdemo.config.BasketLocalDemoDataProperties;
import com.zhs.basket.localdemo.constant.BasketLocalDemoConstants;
import com.zhs.basket.localdemo.mapper.BasketLocalDemoDataMapper;
import com.zhs.basket.localdemo.resp.BasketLocalDemoStatusResp;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "cnpa.local-demo", name = "enabled", havingValue = "true")
public class LocalDemoStatusHelper {

    private final BasketLocalDemoDataProperties properties;
    private final BasketLocalDemoDataMapper demoMapper;

    public LocalDemoStatusHelper(BasketLocalDemoDataProperties properties, BasketLocalDemoDataMapper demoMapper) {
        this.properties = properties;
        this.demoMapper = demoMapper;
    }

    public void fillCounts(BasketLocalDemoStatusResp resp) {
        String prefix = BasketLocalDemoConstants.BIZ_PREFIX;
        String marker = properties.getMarker();
        resp.setAccountCount(demoMapper.countAccounts(prefix));
        resp.setAvatarMissingCount(demoMapper.countDemoMembersMissingAvatar(prefix));
        resp.setAvatarConfiguredCount(Math.max(0, resp.getAccountCount() - resp.getAvatarMissingCount()));
        resp.setPlayerCount(demoMapper.countPlayers(prefix));
        resp.setCaptainCount(demoMapper.countCaptainQualifications(prefix, marker));
        resp.setTeamCount(demoMapper.countTeams(prefix));
        resp.setTeamMemberCount(demoMapper.countTeamMembers(prefix));
        resp.setTournamentCount(demoMapper.countTournaments(prefix));
        resp.setSeasonCount(demoMapper.countSeasons(prefix));
        resp.setRegistrationCount(demoMapper.countRegistrations(prefix, marker));
        resp.setRegistrationPlayerCount(demoMapper.countRegistrationPlayers(prefix));
        resp.setStageCount(demoMapper.countStages(prefix));
        resp.setRoundCount(demoMapper.countRounds(prefix));
        resp.setGroupCount(demoMapper.countGroups(prefix));
        resp.setGroupTeamCount(demoMapper.countGroupTeams(prefix));
        resp.setMatchCount(demoMapper.countMatches(prefix));
        resp.setConfirmedMatchCount(demoMapper.countConfirmedMatches(prefix));
        resp.setScheduledMatchCount(demoMapper.countScheduledMatches(prefix));
        resp.setLineupCount(demoMapper.countLineups(prefix));
        resp.setPeriodScoreCount(demoMapper.countPeriodScores(prefix));
        resp.setTeamMatchStatCount(demoMapper.countTeamMatchStats(prefix));
        resp.setPlayerMatchStatCount(demoMapper.countPlayerMatchStats(prefix));
        resp.setAnnouncementCount(demoMapper.countAnnouncements(prefix));
        resp.setRecommendationCount(demoMapper.countBanners(prefix));

        Long seasonId = demoMapper.selectIdBySeasonNo(BasketLocalDemoConstants.SEASON_NO);
        if (seasonId != null) {
            resp.setStandingCount(demoMapper.countStandingsBySeasonId(seasonId));
            resp.setTeamSeasonStatCount(demoMapper.countTeamSeasonStatsBySeasonId(seasonId));
            resp.setPlayerSeasonStatCount(demoMapper.countPlayerSeasonStatsBySeasonId(seasonId));
            resp.setTeamRankCount(demoMapper.countTeamRanks(seasonId));
            resp.setPlayerRankCount(demoMapper.countPlayerRanks(seasonId));
        }
    }

    /** 核心校验，用于 status 与 seed 完成后的数据完整性验证。 */
    public List<String> evaluateMissing(BasketLocalDemoStatusResp resp) {
        List<String> missing = new ArrayList<>();
        if (resp.getAccountCount() != BasketLocalDemoConstants.PLAYER_COUNT) {
            missing.add("1. 账号数量应为" + BasketLocalDemoConstants.PLAYER_COUNT
                    + "，当前=" + resp.getAccountCount());
        }
        if (resp.getPlayerCount() != BasketLocalDemoConstants.PLAYER_COUNT) {
            missing.add("2. 球员数量应为" + BasketLocalDemoConstants.PLAYER_COUNT
                    + "，当前=" + resp.getPlayerCount());
        }
        if (resp.getCaptainCount() != BasketLocalDemoConstants.CAPTAIN_COUNT) {
            missing.add("4. 队长资格数量应为" + BasketLocalDemoConstants.CAPTAIN_COUNT
                    + "，当前=" + resp.getCaptainCount());
        }
        if (resp.getTeamCount() != BasketLocalDemoConstants.TEAM_COUNT) {
            missing.add("5. 球队数量应为" + BasketLocalDemoConstants.TEAM_COUNT
                    + "，当前=" + resp.getTeamCount());
        }
        if (resp.getTeamMemberCount() != BasketLocalDemoConstants.PLAYER_COUNT) {
            missing.add("6. 每队成员合计应为" + BasketLocalDemoConstants.PLAYER_COUNT
                    + "，当前=" + resp.getTeamMemberCount());
        }
        if (resp.getRegistrationCount() != BasketLocalDemoConstants.TEAM_COUNT) {
            missing.add("9. 报名数量应为" + BasketLocalDemoConstants.TEAM_COUNT
                    + "，当前=" + resp.getRegistrationCount());
        }
        if (resp.getRegistrationPlayerCount() != BasketLocalDemoConstants.PLAYER_COUNT) {
            missing.add("10. 每个报名阵容8人合计应为" + BasketLocalDemoConstants.PLAYER_COUNT
                    + "，当前=" + resp.getRegistrationPlayerCount());
        }
        if (resp.getStageCount() != 1) {
            missing.add("13. 阶段数量应为1，当前=" + resp.getStageCount());
        }
        if (resp.getRoundCount() != BasketLocalDemoConstants.ROUND_COUNT) {
            missing.add("14. 轮次数量应为" + BasketLocalDemoConstants.ROUND_COUNT
                    + "，当前=" + resp.getRoundCount());
        }
        if (resp.getGroupCount() != BasketLocalDemoConstants.GROUP_COUNT) {
            missing.add("15. 小组数量应为" + BasketLocalDemoConstants.GROUP_COUNT
                    + "，当前=" + resp.getGroupCount());
        }
        if (resp.getGroupTeamCount() != BasketLocalDemoConstants.TEAM_COUNT) {
            missing.add("16. 小组球队数量应为" + BasketLocalDemoConstants.TEAM_COUNT
                    + "，当前=" + resp.getGroupTeamCount());
        }
        if (resp.getMatchCount() != BasketLocalDemoConstants.MATCH_COUNT) {
            missing.add("17. 比赛数量应为" + BasketLocalDemoConstants.MATCH_COUNT
                    + "，当前=" + resp.getMatchCount());
        }
        if (resp.getConfirmedMatchCount() != BasketLocalDemoConstants.CONFIRMED_MATCH_COUNT) {
            missing.add("18. 已确认比赛数量应为" + BasketLocalDemoConstants.CONFIRMED_MATCH_COUNT
                    + "，当前=" + resp.getConfirmedMatchCount());
        }
        if (resp.getScheduledMatchCount() != BasketLocalDemoConstants.SCHEDULED_MATCH_COUNT) {
            missing.add("19. 已安排比赛数量应为" + BasketLocalDemoConstants.SCHEDULED_MATCH_COUNT
                    + "，当前=" + resp.getScheduledMatchCount());
        }
        if (resp.getLineupCount() < BasketLocalDemoConstants.LINEUP_COUNT) {
            missing.add("21. 每场比赛双方各8人名单不足，当前=" + resp.getLineupCount());
        }
        if (resp.getPeriodScoreCount() < BasketLocalDemoConstants.PERIOD_SCORE_COUNT) {
            missing.add("23. 已确认比赛4节比分不足，当前=" + resp.getPeriodScoreCount());
        }
        if (resp.getPlayerMatchStatCount() < BasketLocalDemoConstants.PLAYER_MATCH_STAT_COUNT) {
            missing.add("25. 球员比赛统计不足，当前=" + resp.getPlayerMatchStatCount());
        }
        if (resp.getTeamMatchStatCount() < BasketLocalDemoConstants.TEAM_MATCH_STAT_COUNT) {
            missing.add("26. 球队比赛统计不足，当前=" + resp.getTeamMatchStatCount());
        }
        if (resp.getStandingCount() < BasketLocalDemoConstants.STANDING_MIN_COUNT) {
            missing.add("27. 积分榜应包含" + BasketLocalDemoConstants.STANDING_MIN_COUNT
                    + "支球队，当前=" + resp.getStandingCount());
        }
        if (resp.getTeamSeasonStatCount() < BasketLocalDemoConstants.TEAM_SEASON_STAT_MIN) {
            missing.add("29. 球队赛季统计不足，当前=" + resp.getTeamSeasonStatCount());
        }
        if (resp.getPlayerSeasonStatCount() < BasketLocalDemoConstants.PLAYER_SEASON_STAT_MIN) {
            missing.add("30. 球员赛季统计不足，当前=" + resp.getPlayerSeasonStatCount());
        }
        if (resp.getTeamRankCount() < 1) {
            missing.add("31. 球队排行榜不存在");
        }
        if (resp.getPlayerRankCount() < 1) {
            missing.add("32. 球员排行榜不存在");
        }
        if (resp.getRecommendationCount() < 3) {
            missing.add("33. 首页运营推荐数量应至少为3，当前=" + resp.getRecommendationCount());
        }
        if (resp.getAnnouncementCount() < 3) {
            missing.add("34. 已发布公告数量应至少为3，当前=" + resp.getAnnouncementCount());
        }
        return missing;
    }

    public boolean isComplete(BasketLocalDemoStatusResp resp) {
        return evaluateMissing(resp).isEmpty();
    }
}
