package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.vo.v4.StagePairingEntryVo;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureGenerationPrerequisiteValidatorTest {

    @Mock private IBasketV4RuleVersionService ruleVersionService;
    @Mock private IBasketV4RosterVersionService rosterVersionService;
    @Mock private TeamRosterSupport teamRosterSupport;
    @Mock private IStagePairingExecutionApplicationService pairingExecutionApplicationService;

    @InjectMocks
    private StructureGenerationPrerequisiteValidator validator;

    private BasketDivision division;
    private BasketDivisionStage stage;

    @BeforeEach
    void setUp() {
        division = new BasketDivision();
        division.setId(174L);
        division.setDivisionStatus("REGISTRATION_OPEN");
        stage = new BasketDivisionStage();
        stage.setId(637L);
        stage.setDivisionId(174L);
        stage.setSeedRule("ADMIN_SEED");
        stage.setDrawRule("RANDOM_CONFIRM");
        stage.setPairingRule("FIXED_BRACKET");
    }

    @Test
    void versionedOpenRegistration_pairingReady_allowsGenerate() {
        stubVersionedLocked();
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(16));
        when(pairingExecutionApplicationService.getCurrent(637L)).thenReturn(confirmedPairing(16, 4));

        assertDoesNotThrow(() -> validator.assertCanGenerate(division, stage));
        assertEquals(16, teamRosterSupport.selectFormalEventTeams(174L).size());
    }

    @Test
    void seedNotSet_rejected() {
        stubVersionedLocked();
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(16));
        StagePairingExecutionVo pairing = new StagePairingExecutionVo();
        pairing.setStatus("DRAFT");
        when(pairingExecutionApplicationService.getCurrent(637L)).thenReturn(pairing);

        ServiceException ex = assertThrows(ServiceException.class, () -> validator.assertCanGenerate(division, stage));
        assertEquals(StructureGenerationPrerequisiteValidator.MSG_SEED_NOT_SET, ex.getMessage());
    }

    @Test
    void drawNotConfirmed_rejected() {
        stubVersionedLocked();
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(16));
        StagePairingExecutionVo pairing = confirmedPairing(16, 4);
        pairing.setStatus("DRAFT");
        when(pairingExecutionApplicationService.getCurrent(637L)).thenReturn(pairing);

        ServiceException ex = assertThrows(ServiceException.class, () -> validator.assertCanGenerate(division, stage));
        assertEquals(StructureGenerationPrerequisiteValidator.MSG_DRAW_NOT_CONFIRMED, ex.getMessage());
    }

    @Test
    void rosterDraft_rejected() {
        when(ruleVersionService.getCurrent(174L)).thenReturn(lockedVersionedRule());
        when(rosterVersionService.getCurrentLockedVersion(174L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> validator.assertCanGenerate(division, stage));
        assertEquals(StructureGenerationPrerequisiteValidator.MSG_ROSTER_NOT_LOCKED, ex.getMessage());
    }

    @Test
    void formalOne_rejected() {
        stubVersionedLocked();
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(1));

        ServiceException ex = assertThrows(ServiceException.class, () -> validator.assertCanGenerate(division, stage));
        assertEquals(StructureGenerationPrerequisiteValidator.MSG_FORMAL_INSUFFICIENT, ex.getMessage());
    }

    @Test
    void twentyApprovedSixteenFormal_participantsAreFormalOnly() {
        stubVersionedLocked();
        when(teamRosterSupport.selectFormalEventTeams(174L)).thenReturn(formalTeams(16));
        when(pairingExecutionApplicationService.getCurrent(637L)).thenReturn(confirmedPairing(16, 4));

        validator.assertCanGenerate(division, stage);
        assertEquals(16, teamRosterSupport.selectFormalEventTeams(174L).size());
    }

    private void stubVersionedLocked() {
        when(ruleVersionService.getCurrent(174L)).thenReturn(lockedVersionedRule());
        BasketDivisionRosterVersion roster = new BasketDivisionRosterVersion();
        roster.setId(2L);
        roster.setStatus("LOCKED");
        roster.setFormalCount(16);
        roster.setCandidateCount(4);
        when(rosterVersionService.getCurrentLockedVersion(174L)).thenReturn(roster);
    }

    private static RuleVersionView lockedVersionedRule() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRuleVersionId(268L);
        rule.setLocked(true);
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.VERSIONED.getCode());
        return rule;
    }

    private static List<BasketEventTeam> formalTeams(int count) {
        List<BasketEventTeam> teams = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            BasketEventTeam team = new BasketEventTeam();
            team.setId((long) i);
            team.setTeamNameSnapshot("FORMAL-" + i);
            teams.add(team);
        }
        return teams;
    }

    private static StagePairingExecutionVo confirmedPairing(int participants, int seeds) {
        StagePairingExecutionVo vo = new StagePairingExecutionVo();
        vo.setStatus("CONFIRMED");
        vo.setParticipantCount(participants);
        List<StagePairingEntryVo> entries = new ArrayList<>();
        for (int i = 1; i <= participants; i++) {
            StagePairingEntryVo entry = new StagePairingEntryVo();
            entry.setEventTeamId((long) i);
            entry.setSlotNo(i);
            if (i <= seeds) {
                entry.setSeedNo(i);
            }
            entries.add(entry);
        }
        vo.setEntries(entries);
        return vo;
    }
}
