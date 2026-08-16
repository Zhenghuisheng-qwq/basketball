package com.zhs.support.v4;

import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketPublicDisciplineRow;
import com.zhs.response.publicapi.PublicActiveDisciplineSummaryVo;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PublicDisciplineDisplaySupportTest {

    @Test
    void eventTeamDqUsesEventScopedCopy() {
        PublicActiveDisciplineSummaryVo vo = PublicDisciplineDisplaySupport.toSummary(activeRow(
                PublicDisciplineDisplaySupport.TARGET_EVENT_TEAM,
                PublicDisciplineDisplaySupport.CODE_EVENT_DISQUALIFICATION), new Date());
        assertNotNull(vo);
        assertEquals("取消赛事资格", vo.getPenaltyLabel());
        assertEquals("赛事资格已取消", vo.getTagLabel());
        assertEquals("该球队在本届赛事中的参赛资格已取消。", vo.getDescription());
        assertNull(vo.getPublicReason());
    }

    @Test
    void playerPenaltyDoesNotUseTeamDqTag() {
        PublicActiveDisciplineSummaryVo vo = PublicDisciplineDisplaySupport.toSummary(activeRow(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION"), new Date());
        assertNotNull(vo);
        assertEquals("停赛中", vo.getTagLabel());
        assertFalse(vo.getTagLabel().contains("球队"));
        assertEquals(PublicDisciplineDisplaySupport.playerTeamDqTag(), "所属球队赛事资格已取消");
    }

    @Test
    void revokedOrExpiredNeverMapsToActiveSummary() {
        BasketPublicDisciplineRow revoked = activeRow(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION");
        revoked.setRecordStatus("REVOKED");
        assertNull(PublicDisciplineDisplaySupport.toSummary(revoked, new Date()));

        BasketPublicDisciplineRow expired = activeRow(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        expired.setEffectiveEndTime(cal.getTime());
        assertNull(PublicDisciplineDisplaySupport.toSummary(expired, new Date()));
    }

    @Test
    void synthesizeEventTeamDqKeepsDivisionContext() {
        BasketEventTeam team = new BasketEventTeam();
        team.setId(88L);
        team.setDivisionId(12L);
        team.setSeasonId(34L);
        team.setEventTeamStatus("DISQUALIFIED");
        PublicActiveDisciplineSummaryVo vo = PublicDisciplineDisplaySupport.synthesizeEventTeamDq(team);
        assertEquals(Long.valueOf(12L), vo.getDivisionId());
        assertEquals(Long.valueOf(34L), vo.getSeasonId());
        assertEquals(PublicDisciplineDisplaySupport.TARGET_EVENT_TEAM, vo.getTargetType());
        assertNull(vo.getPublicReason());
    }

    @Test
    void playerMatchHistoryUsesTeamForfeitCopy() {
        assertEquals("球队取消资格判负", PublicDisciplineDisplaySupport.playerTeamForfeitLabel(true));
        assertEquals("对手取消资格判负", PublicDisciplineDisplaySupport.playerTeamForfeitLabel(false));
    }

    @Test
    void publicPlayerHeroShowsActiveMatchSuspension() {
        PublicActiveDisciplineSummaryVo vo = PublicDisciplineDisplaySupport.toSummary(activeRow(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION"), new Date());
        assertNotNull(vo);
        assertEquals("停赛中", vo.getTagLabel());
        assertNull(vo.getPublicReason());
        assertTrue(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION"));
    }

    @Test
    void accountPenaltyIsHiddenFromPublicPlayerHero() {
        assertFalse(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                PublicDisciplineDisplaySupport.TARGET_ACCOUNT, "LOGIN_BAN"));
        assertFalse(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                PublicDisciplineDisplaySupport.TARGET_ACCOUNT, "MATCH_SUSPENSION"));
        assertTrue(PublicDisciplineDisplaySupport.PUBLIC_PLAYER_ACCOUNT_PENALTY_CODES.isEmpty());
    }

    @Test
    void playerPlusAccountOnlyExposesPlayerPenalty() {
        assertTrue(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION"));
        assertFalse(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                PublicDisciplineDisplaySupport.TARGET_ACCOUNT, "LOGIN_BAN"));
        PublicActiveDisciplineSummaryVo player = PublicDisciplineDisplaySupport.toSummary(activeRow(
                PublicDisciplineDisplaySupport.TARGET_PLAYER, "MATCH_SUSPENSION"), new Date());
        PublicActiveDisciplineSummaryVo account = PublicDisciplineDisplaySupport.toSummary(activeRow(
                PublicDisciplineDisplaySupport.TARGET_ACCOUNT, "LOGIN_BAN"), new Date());
        assertNotNull(player);
        assertEquals("停赛中", player.getTagLabel());
        assertNull(player.getPublicReason());
        if (account != null) {
            assertFalse(PublicDisciplineDisplaySupport.isPublicPlayerDiscipline(
                    account.getTargetType(), account.getPenaltyCode()));
        }
    }

    @Test
    void currentlyActiveRejectsFutureStart() {
        BasketPublicDisciplineRow row = activeRow(
                PublicDisciplineDisplaySupport.TARGET_PERMANENT_TEAM, "TEAM_SUSPENSION");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, 2);
        row.setEffectiveStartTime(cal.getTime());
        assertFalse(PublicDisciplineDisplaySupport.isCurrentlyActive(row, new Date()));
        assertTrue(PublicDisciplineDisplaySupport.isCurrentlyActive(activeRow(
                PublicDisciplineDisplaySupport.TARGET_PERMANENT_TEAM, "TEAM_SUSPENSION"), new Date()));
    }

    private static BasketPublicDisciplineRow activeRow(String targetType, String penaltyCode) {
        BasketPublicDisciplineRow row = new BasketPublicDisciplineRow();
        row.setId(1L);
        row.setTargetType(targetType);
        row.setPenaltyCode(penaltyCode);
        row.setRecordStatus("ACTIVE");
        row.setEffectiveStartTime(new Date(System.currentTimeMillis() - 60_000L));
        return row;
    }
}
