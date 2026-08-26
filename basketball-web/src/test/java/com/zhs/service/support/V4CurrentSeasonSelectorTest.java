package com.zhs.service.support;

import com.zhs.enums.v4.SeasonStatusEnum;
import com.zhs.model.v4.BasketSeason;
import com.zhs.response.v4.publicapi.V4PublicSeasonItemResp;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class V4CurrentSeasonSelectorTest {

    @Test
    void playingBeatsRegistrationOpenEvenIfOlder() {
        BasketSeason playing = season(1L, SeasonStatusEnum.PLAYING.getCode(), daysAgo(30));
        BasketSeason open = season(2L, SeasonStatusEnum.REGISTRATION_OPEN.getCode(), daysAgo(1));
        assertEquals(1L, V4CurrentSeasonSelector.pickCurrent(List.of(open, playing)).getId());
    }

    @Test
    void registrationOpenBeatsFinished() {
        BasketSeason finished = season(8L, SeasonStatusEnum.FINISHED.getCode(), daysAgo(2));
        BasketSeason open = season(9L, SeasonStatusEnum.REGISTRATION_OPEN.getCode(), daysAgo(10));
        assertEquals(9L, V4CurrentSeasonSelector.pickCurrent(List.of(finished, open)).getId());
    }

    @Test
    void sameStatusPrefersLatestMatchStartThenLatestId() {
        Date start = daysAgo(3);
        BasketSeason olderId = season(10L, SeasonStatusEnum.PLAYING.getCode(), start);
        BasketSeason newerId = season(11L, SeasonStatusEnum.PLAYING.getCode(), start);
        assertEquals(11L, V4CurrentSeasonSelector.pickCurrent(List.of(olderId, newerId)).getId());
    }

    @Test
    void doesNotUseStringDictionaryOrder() {
        assertEquals(0, V4CurrentSeasonSelector.statusPriority(SeasonStatusEnum.PLAYING.getCode()));
        assertEquals(1, V4CurrentSeasonSelector.statusPriority(SeasonStatusEnum.REGISTRATION_OPEN.getCode()));
        assertEquals(4, V4CurrentSeasonSelector.statusPriority(SeasonStatusEnum.FINISHED.getCode()));
    }

    @Test
    void pickCurrentBySeriesCodeReturnsOnePerSeries() {
        V4PublicSeasonItemResp leaguePlaying = publicSeason(1L, "LEAGUE", SeasonStatusEnum.PLAYING.getCode(), daysAgo(5));
        V4PublicSeasonItemResp leagueFinished = publicSeason(2L, "LEAGUE", SeasonStatusEnum.FINISHED.getCode(), daysAgo(1));
        V4PublicSeasonItemResp cupOpen = publicSeason(3L, "CUP", SeasonStatusEnum.REGISTRATION_OPEN.getCode(), daysAgo(2));
        Map<String, V4PublicSeasonItemResp> picked = V4CurrentSeasonSelector.pickCurrentBySeriesCode(
                List.of(leagueFinished, leaguePlaying, cupOpen),
                List.of("LEAGUE", "CUP", "ARENA", "OPEN"));
        assertEquals(1L, picked.get("LEAGUE").getSeasonId());
        assertEquals(3L, picked.get("CUP").getSeasonId());
        assertNull(picked.get("ARENA"));
        assertNull(picked.get("OPEN"));
    }

    @Test
    void homeFourSeriesUsesStatusPriorityAndLeavesMissingOpenEmpty() {
        V4PublicSeasonItemResp leaguePlaying = publicSeason(1L, "LEAGUE", SeasonStatusEnum.PLAYING.getCode(), daysAgo(2));
        V4PublicSeasonItemResp leagueFinished = publicSeason(11L, "LEAGUE", SeasonStatusEnum.FINISHED.getCode(), daysAgo(1));
        V4PublicSeasonItemResp cupOpen = publicSeason(2L, "CUP", SeasonStatusEnum.REGISTRATION_OPEN.getCode(), daysAgo(3));
        V4PublicSeasonItemResp arenaNotStarted = publicSeason(3L, "ARENA", SeasonStatusEnum.NOT_STARTED.getCode(), daysAgo(4));
        Map<String, V4PublicSeasonItemResp> picked = V4CurrentSeasonSelector.pickCurrentBySeriesCode(
                List.of(leagueFinished, cupOpen, arenaNotStarted, leaguePlaying),
                List.of("LEAGUE", "CUP", "ARENA", "OPEN"));
        assertEquals(1L, picked.get("LEAGUE").getSeasonId());
        assertEquals(2L, picked.get("CUP").getSeasonId());
        assertEquals(3L, picked.get("ARENA").getSeasonId());
        assertNull(picked.get("OPEN"));
    }

    private static BasketSeason season(Long id, String status, Date start) {
        BasketSeason season = new BasketSeason();
        season.setId(id);
        season.setSeasonStatus(status);
        season.setMatchStartTime(start);
        return season;
    }

    private static V4PublicSeasonItemResp publicSeason(Long id, String seriesCode, String status, Date start) {
        V4PublicSeasonItemResp season = new V4PublicSeasonItemResp();
        season.setSeasonId(id);
        season.setSeriesCode(seriesCode);
        season.setSeasonStatus(status);
        season.setMatchStartTime(start);
        return season;
    }

    private static Date daysAgo(int days) {
        return new Date(System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L);
    }
}
