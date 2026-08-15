package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.enums.v4.MatchPublishStatusEnum;
import com.zhs.basket.enums.v4.MatchSeriesStatusEnum;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.SeriesResolutionTypeEnum;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminOfficialResultCapabilityTest {

    @Test
    void enter_onlyPublishedUnsettledWithTeams() {
        BasketV4Match match = published(MatchStatusEnum.SCHEDULED);
        assertTrue(AdminOfficialResultCapability.canEnterOfficialResult(match));
        assertTrue(AdminOfficialResultCapability.canForfeitMatch(match));
        assertFalse(AdminOfficialResultCapability.canCorrectOfficialResult(match));
    }

    @Test
    void revokeForfeit_onlyForfeited() {
        BasketV4Match match = published(MatchStatusEnum.FORFEITED);
        assertTrue(AdminOfficialResultCapability.canRevokeMatchForfeit(match));
        assertFalse(AdminOfficialResultCapability.canForfeitMatch(match));
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        assertFalse(AdminOfficialResultCapability.canRevokeMatchForfeit(match));
    }

    @Test
    void correct_onlyFinished() {
        BasketV4Match match = published(MatchStatusEnum.FINISHED);
        assertFalse(AdminOfficialResultCapability.canEnterOfficialResult(match));
        assertTrue(AdminOfficialResultCapability.canCorrectOfficialResult(match));
        assertTrue(AdminOfficialResultCapability.canAdminResolveResult(match));
    }

    @Test
    void seriesManual_hiddenAfterForfeitOrManual() {
        BasketMatchSeries series = new BasketMatchSeries();
        series.setHomeEventTeamId(1L);
        series.setAwayEventTeamId(2L);
        series.setSeriesStatus(MatchSeriesStatusEnum.RUNNING.getCode());
        assertTrue(AdminOfficialResultCapability.canResolveSeriesManually(series));
        series.setResolutionType(SeriesResolutionTypeEnum.FORFEIT.getCode());
        assertFalse(AdminOfficialResultCapability.canResolveSeriesManually(series));
    }

    private static BasketV4Match published(MatchStatusEnum status) {
        BasketV4Match match = new BasketV4Match();
        match.setHomeEventTeamId(1L);
        match.setAwayEventTeamId(2L);
        match.setPublishStatus(MatchPublishStatusEnum.PUBLISHED.getCode());
        match.setMatchStatus(status.getCode());
        return match;
    }
}
