package com.zhs.basket.support.v4.template;

import com.zhs.basket.dto.v4.BasketDivisionFormatPlanApplyReq;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.FormatPlanCodeEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.support.v4.DivisionFormatPlanValidator;
import com.zhs.basket.support.v4.StandingPointsRule;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandardTemplateScoringRuleTest {

    private final LeagueStandardTemplateBuilder league = new LeagueStandardTemplateBuilder();
    private final CupStandardTemplateBuilder cup = new CupStandardTemplateBuilder();
    private final ArenaStandardTemplateBuilder arena = new ArenaStandardTemplateBuilder();
    private final OpenStandardTemplateBuilder open = new OpenStandardTemplateBuilder();

    @Test
    void leagueDefaultPointsMatchCustomLegacy() {
        StandardTemplateDefinition def = league.build(teamContext());
        StandardTemplateStageDefinition regular = def.getStages().get(0);
        StandardTemplateStageDefinition playoff = def.getStages().get(1);
        assertEquals(StandingPointsRule.LEGACY_WIN_POINTS, regular.getWinPoints());
        assertEquals(StandingPointsRule.LEGACY_LOSS_POINTS, regular.getLossPoints());
        assertEquals(StandingPointsRule.LEGACY_WIN_POINTS, regular.getForfeitWinPoints());
        assertEquals(StandingPointsRule.LEGACY_LOSS_POINTS, regular.getForfeitLossPoints());
        assertNull(playoff.getWinPoints());
        assertNull(playoff.getLossPoints());
        assertTrue(def.getOptions().stream().anyMatch(o -> "winPoints".equals(o.getKey())));
        assertTrue(def.getOptions().stream().anyMatch(o -> "lossPoints".equals(o.getKey())));
    }

    @Test
    void leagueCustomPointsStayOnRoundRobinOnly() {
        StandardTemplateContext context = teamContext();
        context.setWinPoints(3);
        context.setLossPoints(1);
        StandardTemplateDefinition def = league.build(context);
        assertEquals(3, def.getStages().get(0).getWinPoints());
        assertEquals(1, def.getStages().get(0).getLossPoints());
        assertNull(def.getStages().get(1).getWinPoints());
        assertNull(def.getStages().get(1).getLossPoints());
    }

    @Test
    void leagueZeroLossPointsAllowed() {
        StandardTemplateContext context = teamContext();
        context.setWinPoints(2);
        context.setLossPoints(0);
        StandardTemplateDefinition def = league.build(context);
        assertEquals(2, def.getStages().get(0).getWinPoints());
        assertEquals(0, def.getStages().get(0).getLossPoints());
    }

    @Test
    void leagueZeroZeroAllowedLikeCustom() {
        StandardTemplateContext context = teamContext();
        context.setWinPoints(0);
        context.setLossPoints(0);
        StandardTemplateDefinition def = league.build(context);
        assertEquals(0, def.getStages().get(0).getWinPoints());
        assertEquals(0, def.getStages().get(0).getLossPoints());
    }

    @Test
    void leagueRejectsNegativePoints() {
        StandardTemplateContext context = teamContext();
        context.setWinPoints(-1);
        context.setLossPoints(1);
        ServiceException ex = assertThrows(ServiceException.class, () -> league.build(context));
        assertTrue(ex.getMessage().contains("winPoints"));
    }

    @Test
    void cupDefaultAndCustomPointsOnGroupOnly() {
        StandardTemplateDefinition defaults = cup.build(teamContext());
        assertEquals(2, defaults.getStages().get(0).getWinPoints());
        assertEquals(1, defaults.getStages().get(0).getLossPoints());
        assertNull(defaults.getStages().get(1).getWinPoints());

        StandardTemplateContext context = teamContext();
        context.setWinPoints(2);
        context.setLossPoints(0);
        StandardTemplateDefinition custom = cup.build(context);
        assertEquals(2, custom.getStages().get(0).getWinPoints());
        assertEquals(0, custom.getStages().get(0).getLossPoints());
        assertNull(custom.getStages().get(1).getWinPoints());
        assertTrue(defaults.getOptions().stream().anyMatch(o -> "winPoints".equals(o.getKey())));
    }

    @Test
    void arenaAndOpenHaveNoStandingPointsOptionsOrStageValues() {
        StandardTemplateDefinition arenaDef = arena.build(teamContext());
        StandardTemplateDefinition openDef = open.build(personalContext());
        assertFalse(arenaDef.getOptions().stream().anyMatch(o -> "winPoints".equals(o.getKey()) || "lossPoints".equals(o.getKey())));
        assertFalse(openDef.getOptions().stream().anyMatch(o -> "winPoints".equals(o.getKey()) || "lossPoints".equals(o.getKey())));
        assertNull(arenaDef.getStages().get(0).getWinPoints());
        assertNull(arenaDef.getStages().get(0).getLossPoints());
        assertNull(openDef.getStages().get(0).getWinPoints());
        assertNull(openDef.getStages().get(0).getLossPoints());
        assertFalse(StandingPointsRule.supportsCompetitionFormat(arenaDef.getStages().get(0).getCompetitionFormat()));
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), arenaDef.getStages().get(0).getCompetitionFormat());
    }

    @Test
    void customFormatPlanKeepsSameValidationAsStandard() {
        DivisionFormatPlanValidator validator = new DivisionFormatPlanValidator();
        BasketDivisionFormatPlanApplyReq rr = new BasketDivisionFormatPlanApplyReq();
        rr.setPlanCode(FormatPlanCodeEnum.ROUND_ROBIN_TO_ELIM.getCode());
        rr.setExpectedTeamCount(8);
        rr.setOverallAdvanceCount(4);
        rr.setWinPoints(0);
        rr.setLossPoints(0);
        assertTrue(validator.validate(rr).isValid());

        BasketDivisionFormatPlanApplyReq group = new BasketDivisionFormatPlanApplyReq();
        group.setPlanCode(FormatPlanCodeEnum.GROUP_TO_ELIM.getCode());
        group.setExpectedTeamCount(8);
        group.setGroupCount(2);
        group.setQualifiersPerGroup(2);
        group.setWinPoints(2);
        group.setLossPoints(0);
        assertTrue(validator.validate(group).isValid());

        rr.setWinPoints(-1);
        assertFalse(validator.validate(rr).isValid());
    }

    @Test
    void leftoverArenaContextPointsAreNotWritten() {
        StandardTemplateContext context = teamContext();
        context.setWinPoints(9);
        context.setLossPoints(8);
        StandardTemplateDefinition def = arena.build(context);
        assertNull(def.getStages().get(0).getWinPoints());
        assertNull(def.getStages().get(0).getLossPoints());
    }

    @Test
    void standingCapabilityIsFormatBasedNotTemplateBased() {
        assertTrue(StandingPointsRule.supportsCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode()));
        assertFalse(StandingPointsRule.supportsCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode()));
        assertEquals(2, StandingPointsRule.winPointsOrLegacy(null));
        assertEquals(1, StandingPointsRule.lossPointsOrLegacy(null));
        assertEquals(0, StandingPointsRule.lossPointsOrLegacy(0));
    }

    private static StandardTemplateContext teamContext() {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        return context;
    }

    private static StandardTemplateContext personalContext() {
        StandardTemplateContext context = new StandardTemplateContext();
        context.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        return context;
    }
}
