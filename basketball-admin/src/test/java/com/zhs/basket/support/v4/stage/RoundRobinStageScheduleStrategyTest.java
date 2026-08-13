package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoundRobinStageScheduleStrategyTest {

    private RoundRobinStageScheduleStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new RoundRobinStageScheduleStrategy();
    }

    @Test
    void preview_fourTeams_sixMatchesThreePerTeam() {
        StageExecutionContext context = buildContext(4, 1);

        StageSchedulePreviewVo preview = strategy.preview(context);

        assertEquals(4, preview.getTeamCount());
        assertEquals(6, preview.getTotalMatches());
        assertEquals(3, preview.getMatchesPerTeam());
        assertEquals(3, preview.getRoundCount());
        assertFalse(preview.getHasBye());
    }

    @Test
    void preview_fiveTeams_tenMatchesWithBye() {
        StageExecutionContext context = buildContext(5, 1);

        StageSchedulePreviewVo preview = strategy.preview(context);

        assertEquals(5, preview.getTeamCount());
        assertEquals(10, preview.getTotalMatches());
        assertEquals(4, preview.getMatchesPerTeam());
        assertEquals(5, preview.getRoundCount());
        assertTrue(preview.getHasBye());
    }

    @Test
    void supports_singleEliminationWith6Teams_returnsFalse() {
        BasketDivisionStage stage = baseStage();
        stage.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        stage.setStageType(StageTypeEnum.KNOCKOUT.getCode());

        assertFalse(strategy.supports(stage));
    }

    @Test
    void validate_singleEliminationWith6Teams_rejectedByEliminationStrategy() {
        SingleEliminationStageScheduleStrategy eliminationStrategy = new SingleEliminationStageScheduleStrategy();
        StageExecutionContext context = buildContext(6, 1);
        context.getStage().setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        context.getStage().setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        context.getStage().setStageType(StageTypeEnum.KNOCKOUT.getCode());

        ServiceException ex = assertThrows(ServiceException.class, () -> eliminationStrategy.validate(context));
        assertTrue(ex.getMessage().contains("2 的幂次"));
    }

    @Test
    void validate_bestOf3_allowed() {
        StageExecutionContext context = buildContext(4, 3);
        strategy.validate(context);
        assertEquals(3, context.getStage().getBestOf());
    }

    @Test
    void preview_fourTeamsDouble_twelvePairingsSixRounds() {
        StageExecutionContext context = buildContext(4, 1);
        context.getStage().setRoundRobinCycles(2);
        StageSchedulePreviewVo preview = strategy.preview(context);
        assertEquals(12, preview.getTotalMatches());
        assertEquals(6, preview.getRoundCount());
    }

    private StageExecutionContext buildContext(int teamCount, int bestOf) {
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(1L);
        division.setPlatformCode("CNPA");
        division.setFormatType("REGULAR_ROUND_ROBIN");

        BasketDivisionStage stage = baseStage();
        stage.setBestOf(bestOf);

        List<BasketDivisionStageTeam> teams = new ArrayList<>();
        for (int i = 1; i <= teamCount; i++) {
            BasketDivisionStageTeam team = new BasketDivisionStageTeam();
            team.setEventTeamId((long) i);
            team.setSeedNo(i);
            teams.add(team);
        }

        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(division);
        context.setStage(stage);
        context.setStageTeams(teams);
        context.setExecutionVersion(1);
        return context;
    }

    private BasketDivisionStage baseStage() {
        BasketDivisionStage stage = new BasketDivisionStage();
        stage.setId(10L);
        stage.setDivisionId(100L);
        stage.setStageCode("REGULAR");
        stage.setStageName("常规赛");
        stage.setStageType(StageTypeEnum.REGULAR.getCode());
        stage.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        stage.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        stage.setGroupingMode(GroupingModeEnum.NONE.getCode());
        stage.setRoundRobinCycles(1);
        stage.setBestOf(1);
        return stage;
    }
}
