package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatchDataSubmission;
import com.zhs.basket.domain.BasketMatchDataSubmissionMaterial;
import com.zhs.basket.domain.BasketMatchDataSubmissionPeriod;
import com.zhs.basket.domain.BasketMatchDataSubmissionPlayerStat;
import com.zhs.basket.domain.BasketMatchDataSubmissionTeamStat;
import com.zhs.basket.enums.MatchPeriodTypeEnum;
import com.zhs.basket.mapper.BasketFileAssetMapper;
import com.zhs.basket.mapper.BasketMatchRosterMapper;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchDataSubmissionValidationServiceImplTest {

    private static final Long MATCH_ID = 100L;
    private static final Long HOME_TEAM_ID = 1L;
    private static final Long AWAY_TEAM_ID = 2L;
    private static final Long PLAYER_HOME = 11L;
    private static final Long PLAYER_AWAY = 21L;

    @Mock
    private IBasketMatchValidationService validationService;
    @Mock
    private BasketMatchRosterMapper rosterMapper;
    @Mock
    private BasketFileAssetMapper fileAssetMapper;

    @InjectMocks
    private BasketMatchDataSubmissionValidationServiceImpl validationServiceImpl;

    private BasketMatchDataSubmission submission;

    @BeforeEach
    void setUp() {
        submission = new BasketMatchDataSubmission();
        submission.setMatchId(MATCH_ID);
        submission.setHomeTeamId(HOME_TEAM_ID);
        submission.setAwayTeamId(AWAY_TEAM_ID);
        submission.setHomeScore(100);
        submission.setAwayScore(90);
    }

    @Test
    void validateSubmissionData_success() {
        when(rosterMapper.countByMatchIdAndPlayerId(MATCH_ID, PLAYER_HOME)).thenReturn(1);
        when(rosterMapper.countByMatchIdAndPlayerId(MATCH_ID, PLAYER_AWAY)).thenReturn(1);
        when(fileAssetMapper.selectById(1001L)).thenReturn(new com.zhs.basket.domain.BasketFileAsset());

        assertDoesNotThrow(() -> validationServiceImpl.validateSubmissionData(
                submission,
                buildPeriods(25, 22, 25, 23, 25, 23, 25, 22),
                buildTeamStats(),
                buildPlayerStats(),
                buildMaterials()));
    }

    @Test
    void validateSubmissionData_rejectsPeriodTotalMismatch() {
        assertThrows(ServiceException.class, () -> validationServiceImpl.validateSubmissionData(
                submission,
                buildPeriods(20, 25, 25, 25, 22, 23, 23, 22),
                buildTeamStats(),
                buildPlayerStats(),
                buildMaterials()));
    }

    @Test
    void validateSubmissionData_rejectsMissingScreenshot() {
        assertThrows(ServiceException.class, () -> validationServiceImpl.validateSubmissionData(
                submission,
                buildPeriods(25, 22, 25, 23, 25, 23, 25, 22),
                buildTeamStats(),
                buildPlayerStats(),
                Collections.emptyList()));
    }

    private List<BasketMatchDataSubmissionPeriod> buildPeriods(int... scores) {
        BasketMatchDataSubmissionPeriod[] periods = new BasketMatchDataSubmissionPeriod[4];
        for (int i = 0; i < 4; i++) {
            BasketMatchDataSubmissionPeriod period = new BasketMatchDataSubmissionPeriod();
            period.setPeriodNo(i + 1);
            period.setPeriodType(MatchPeriodTypeEnum.REGULAR.getCode());
            period.setHomeScore(scores[i * 2]);
            period.setAwayScore(scores[i * 2 + 1]);
            periods[i] = period;
        }
        return Arrays.asList(periods);
    }

    private List<BasketMatchDataSubmissionTeamStat> buildTeamStats() {
        BasketMatchDataSubmissionTeamStat home = new BasketMatchDataSubmissionTeamStat();
        home.setTeamId(HOME_TEAM_ID);
        home.setPoints(100);
        home.setFieldGoalsMade(40);
        home.setFieldGoalsAttempted(80);
        home.setThreePointsMade(10);
        home.setThreePointsAttempted(30);
        home.setFreeThrowsMade(10);
        home.setFreeThrowsAttempted(12);

        BasketMatchDataSubmissionTeamStat away = new BasketMatchDataSubmissionTeamStat();
        away.setTeamId(AWAY_TEAM_ID);
        away.setPoints(90);
        away.setFieldGoalsMade(35);
        away.setFieldGoalsAttempted(75);
        away.setThreePointsMade(8);
        away.setThreePointsAttempted(25);
        away.setFreeThrowsMade(12);
        away.setFreeThrowsAttempted(15);
        return Arrays.asList(home, away);
    }

    private List<BasketMatchDataSubmissionPlayerStat> buildPlayerStats() {
        BasketMatchDataSubmissionPlayerStat homePlayer = new BasketMatchDataSubmissionPlayerStat();
        homePlayer.setTeamId(HOME_TEAM_ID);
        homePlayer.setPlayerId(PLAYER_HOME);
        homePlayer.setPoints(100);
        homePlayer.setFieldGoalsMade(40);
        homePlayer.setFieldGoalsAttempted(80);
        homePlayer.setThreePointsMade(10);
        homePlayer.setThreePointsAttempted(30);
        homePlayer.setFreeThrowsMade(10);
        homePlayer.setFreeThrowsAttempted(12);

        BasketMatchDataSubmissionPlayerStat awayPlayer = new BasketMatchDataSubmissionPlayerStat();
        awayPlayer.setTeamId(AWAY_TEAM_ID);
        awayPlayer.setPlayerId(PLAYER_AWAY);
        awayPlayer.setPoints(90);
        awayPlayer.setFieldGoalsMade(35);
        awayPlayer.setFieldGoalsAttempted(75);
        awayPlayer.setThreePointsMade(8);
        awayPlayer.setThreePointsAttempted(25);
        awayPlayer.setFreeThrowsMade(12);
        awayPlayer.setFreeThrowsAttempted(15);
        return Arrays.asList(homePlayer, awayPlayer);
    }

    private List<BasketMatchDataSubmissionMaterial> buildMaterials() {
        BasketMatchDataSubmissionMaterial material = new BasketMatchDataSubmissionMaterial();
        material.setMaterialType("RESULT_SCREENSHOT");
        material.setFileId(1001L);
        return Collections.singletonList(material);
    }
}
