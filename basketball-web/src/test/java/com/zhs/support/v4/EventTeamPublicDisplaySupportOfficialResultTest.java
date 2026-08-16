package com.zhs.support.v4;

import com.zhs.model.v4.BasketEventTeam;
import com.zhs.model.v4.BasketV4Match;
import com.zhs.response.v4.publicapi.V4PublicMatchDetailResp;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EventTeamPublicDisplaySupportOfficialResultTest {

    @Test
    void forfeitMatchExposesOfficialLabelsWithoutInventedScore() {
        BasketV4Match match = match("FORFEITED", 10L, null);
        V4PublicMatchDetailResp vo = new V4PublicMatchDetailResp();
        vo.setHomeScore(null);
        vo.setAwayScore(null);
        EventTeamPublicDisplaySupport.fillMatchDetail(vo, match, team(10L, "ACTIVE"), team(20L, "ACTIVE"));
        assertEquals(EventTeamPublicDisplaySupport.RESULT_FORFEIT, vo.getResultType());
        assertEquals(EventTeamPublicDisplaySupport.LABEL_FORFEIT_WIN, vo.getHomeResultLabel());
        assertEquals(EventTeamPublicDisplaySupport.LABEL_FORFEIT_LOSS, vo.getAwayResultLabel());
        assertNull(vo.getHomeScore());
        assertNull(vo.getAwayScore());
    }

    @Test
    void dqForfeitUsesDisqualificationResultType() {
        BasketV4Match match = match("FORFEITED", 10L, "EVENT_DQ");
        V4PublicMatchDetailResp vo = new V4PublicMatchDetailResp();
        EventTeamPublicDisplaySupport.fillMatchDetail(vo, match, team(10L, "ACTIVE"), team(20L, "DISQUALIFIED"));
        assertEquals(EventTeamPublicDisplaySupport.RESULT_EVENT_DISQUALIFICATION, vo.getResultType());
        assertEquals(EventTeamPublicDisplaySupport.LABEL_FORFEIT_WIN, vo.getHomeResultLabel());
        assertEquals(EventTeamPublicDisplaySupport.LABEL_FORFEIT_LOSS, vo.getAwayResultLabel());
    }

    @Test
    void finishedMatchKeepsBasketballScore() {
        BasketV4Match match = match("FINISHED", 10L, null);
        match.setHomeScore(65);
        match.setAwayScore(50);
        V4PublicMatchDetailResp vo = new V4PublicMatchDetailResp();
        vo.setHomeScore(65);
        vo.setAwayScore(50);
        EventTeamPublicDisplaySupport.fillMatchDetail(vo, match, team(10L, "ACTIVE"), team(20L, "ACTIVE"));
        assertEquals(65, vo.getHomeScore());
        assertEquals(50, vo.getAwayScore());
        assertNull(vo.getResultType());
    }

    private BasketV4Match match(String status, Long winner, String remark) {
        BasketV4Match match = new BasketV4Match();
        match.setId(1L);
        match.setHomeEventTeamId(10L);
        match.setAwayEventTeamId(20L);
        match.setWinnerEventTeamId(winner);
        match.setMatchStatus(status);
        match.setRemark(remark);
        return match;
    }

    private BasketEventTeam team(Long id, String status) {
        BasketEventTeam team = new BasketEventTeam();
        team.setId(id);
        team.setEventTeamStatus(status);
        return team;
    }
}