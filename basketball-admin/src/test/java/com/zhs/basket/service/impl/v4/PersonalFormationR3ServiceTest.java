package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.BasketMemberAccount;
import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.PersonalFormationMoveReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.BasketMemberAccountMapper;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.support.v4.formation.BalancedRandomFormationStrategy;
import com.zhs.basket.support.v4.formation.FormationStrategyFactory;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PersonalFormationR3ServiceTest {

    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private BasketSeasonMapper seasonMapper;
    @Mock private BasketSeriesMapper seriesMapper;
    @Mock private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Mock private BasketPersonalDrawBatchMapper drawBatchMapper;
    @Mock private BasketPersonalDrawTeamMapper drawTeamMapper;
    @Mock private BasketPersonalDrawAssignmentMapper drawAssignmentMapper;
    @Mock private BasketMemberAccountMapper memberAccountMapper;
    @Mock private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Mock private BasketDivisionRosterEntryMapper rosterEntryMapper;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private IBasketV4RuleVersionService ruleVersionService;
    @Mock private IBasketV4RosterVersionService rosterVersionService;
    @Mock private IBasketV4EventSnapshotService eventSnapshotService;
    @Mock private IBasketV4StructureVersionService structureVersionService;
    @Mock private BasketEventTeamMapper eventTeamMapper;

    @InjectMocks private PersonalFormationCommandServiceImpl formationService;
    @InjectMocks private TeamRosterSupport rosterSupport;

    private final AtomicLong ids = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        FormationStrategyFactory factory = new FormationStrategyFactory(List.of(new BalancedRandomFormationStrategy()));
        ReflectionTestUtils.setField(formationService, "formationStrategyFactory", factory);
        ReflectionTestUtils.setField(rosterSupport, "divisionMapper", divisionMapper);
        ReflectionTestUtils.setField(rosterSupport, "eventTeamMapper", eventTeamMapper);
        ReflectionTestUtils.setField(rosterSupport, "rosterVersionService", rosterVersionService);
        ReflectionTestUtils.setField(rosterSupport, "ruleVersionService", ruleVersionService);
    }

    @Test
    void test1_openPersonalEntersFormation() {
        BasketDivision division = personalDivision("OPEN");
        stubGenerate(division, 6, GameModeEnum.THREE_V_THREE);
        formationService.generate(100L, 9L);
        verify(drawTeamMapper, times(2)).insert(any());
        verify(seriesMapper, never()).selectById(any());
    }

    @Test
    void test2_leaguePersonalAlsoEntersFormation() {
        BasketDivision division = personalDivision("LEAGUE");
        stubGenerate(division, 6, GameModeEnum.THREE_V_THREE);
        formationService.generate(100L, 9L);
        verify(drawTeamMapper, times(2)).insert(any());
    }

    @Test
    void test9_moveRecalculatesTeamSize() {
        BasketDivision division = personalDivision("LEAGUE");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        BasketPersonalDrawBatch batch = editableBatch();
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(batch);
        BasketPersonalRegistration person = approvedPerson(1L, 1L);
        when(personalRegistrationMapper.selectById(1L)).thenReturn(person);
        BasketPersonalDrawAssignment assignment = assignment(1L, 10L, 1L);
        when(drawAssignmentMapper.selectByPersonalRegistrationId(1L)).thenReturn(assignment);
        when(drawTeamMapper.selectById(20L)).thenReturn(team(20L, batch.getId()));
        when(drawTeamMapper.selectByBatchId(batch.getId())).thenReturn(List.of(team(10L, batch.getId()), team(20L, batch.getId())));
        when(personalRegistrationMapper.selectApprovedByDivisionId(100L)).thenReturn(List.of(person, approvedPerson(2L, 2L)));
        when(memberAccountMapper.selectByIds(any())).thenReturn(List.of(account(1L, new Date(1)), account(2L, new Date(2))));
        when(drawAssignmentMapper.selectByTeamId(anyLong())).thenReturn(List.of(assignment));
        when(drawTeamMapper.selectById(anyLong())).thenAnswer(inv -> team(inv.getArgument(0), batch.getId()));
        when(drawTeamMapper.updateById(any())).thenReturn(1);
        when(drawAssignmentMapper.updateById(any())).thenReturn(1);
        when(drawBatchMapper.updateById(any())).thenReturn(1);

        PersonalFormationMoveReq req = new PersonalFormationMoveReq();
        req.setPersonalRegistrationId(1L);
        req.setFromDrawTeamId(10L);
        req.setToDrawTeamId(20L);
        formationService.moveMember(100L, req);
        verify(drawTeamMapper, atLeastOnce()).refreshActualMemberCount(10L);
        verify(drawTeamMapper, atLeastOnce()).refreshActualMemberCount(20L);
    }

    @Test
    void test10_illegalSizeLockRejected() {
        BasketDivision division = personalDivision("LEAGUE");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        stubVersionedRule();
        BasketPersonalDrawBatch batch = editableBatch();
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(batch);
        List<BasketPersonalRegistration> approved = List.of(approvedPerson(1L, 1L), approvedPerson(2L, 2L),
                approvedPerson(3L, 3L), approvedPerson(4L, 4L), approvedPerson(5L, 5L), approvedPerson(6L, 6L));
        when(personalRegistrationMapper.selectApprovedByDivisionId(100L)).thenReturn(approved);
        when(memberAccountMapper.selectByIds(any())).thenReturn(accounts(approved));
        BasketPersonalDrawTeam t1 = team(10L, batch.getId());
        t1.setTemporaryTeamName("A");
        t1.setCaptainPersonalRegistrationId(1L);
        t1.setCaptainPlayerId(1L);
        t1.setCaptainUserId(1L);
        BasketPersonalDrawTeam t2 = team(20L, batch.getId());
        t2.setTemporaryTeamName("B");
        t2.setCaptainPersonalRegistrationId(4L);
        t2.setCaptainPlayerId(4L);
        t2.setCaptainUserId(4L);
        when(drawTeamMapper.selectByBatchId(batch.getId())).thenReturn(List.of(t1, t2));
        when(drawAssignmentMapper.selectByBatchId(batch.getId())).thenReturn(List.of(
                assignment(1L, 10L, 1L), assignment(2L, 10L, 2L), assignment(3L, 10L, 3L), assignment(4L, 10L, 4L),
                assignment(5L, 20L, 5L), assignment(6L, 20L, 6L)));
        when(drawAssignmentMapper.selectByTeamId(10L)).thenReturn(List.of(
                assignment(1L, 10L, 1L), assignment(2L, 10L, 2L), assignment(3L, 10L, 3L), assignment(4L, 10L, 4L)));
        when(drawAssignmentMapper.selectByTeamId(20L)).thenReturn(List.of(assignment(5L, 20L, 5L), assignment(6L, 20L, 6L)));
        when(drawTeamMapper.selectById(10L)).thenReturn(t1);
        when(drawTeamMapper.selectById(20L)).thenReturn(t2);
        when(personalRegistrationMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return approved.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        });
        assertThrows(ServiceException.class, () -> formationService.lock(100L, 9L));
    }

    @Test
    void test11_captainEarliestCreateTimeThenId() {
        com.zhs.basket.support.v4.formation.FormationPlayer a = player(1L, 10L);
        com.zhs.basket.support.v4.formation.FormationPlayer b = player(2L, 20L);
        BasketMemberAccount accA = account(10L, new Date(50));
        BasketMemberAccount accB = account(20L, new Date(50));
        accA.setId(10L);
        accB.setId(20L);
        var captain = com.zhs.basket.support.v4.formation.TemporaryCaptainResolver.resolve(
                List.of(a, b), java.util.Map.of(10L, accA, 20L, accB));
        assertEquals(1L, captain.getPersonalRegistrationId());
    }

    @Test
    void test12_moveCaptainRecalculatesOriginalTeam() {
        BasketDivision division = personalDivision("LEAGUE");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        BasketPersonalDrawBatch batch = editableBatch();
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(batch);
        BasketPersonalRegistration p1 = approvedPerson(1L, 1L);
        BasketPersonalRegistration p2 = approvedPerson(2L, 2L);
        BasketPersonalRegistration p3 = approvedPerson(3L, 3L);
        when(personalRegistrationMapper.selectById(1L)).thenReturn(p1);
        when(personalRegistrationMapper.selectApprovedByDivisionId(100L)).thenReturn(List.of(p1, p2, p3));
        when(memberAccountMapper.selectByIds(any())).thenReturn(List.of(
                account(1L, new Date(1)), account(2L, new Date(2)), account(3L, new Date(3))));
        BasketPersonalDrawAssignment moved = assignment(1L, 10L, 1L);
        when(drawAssignmentMapper.selectByPersonalRegistrationId(1L)).thenReturn(moved);
        when(drawTeamMapper.selectById(20L)).thenReturn(team(20L, batch.getId()));
        when(drawTeamMapper.selectByBatchId(batch.getId())).thenReturn(List.of(team(10L, batch.getId()), team(20L, batch.getId())));
        when(drawAssignmentMapper.selectByTeamId(10L)).thenReturn(List.of(assignment(2L, 10L, 2L)));
        when(drawAssignmentMapper.selectByTeamId(20L)).thenReturn(List.of(assignment(1L, 20L, 1L), assignment(3L, 20L, 3L)));
        when(drawTeamMapper.selectById(10L)).thenReturn(team(10L, batch.getId()));
        when(drawTeamMapper.selectById(anyLong())).thenAnswer(inv -> team(inv.getArgument(0), batch.getId()));
        when(drawTeamMapper.updateById(any())).thenReturn(1);
        when(drawAssignmentMapper.updateById(any())).thenReturn(1);
        when(drawBatchMapper.updateById(any())).thenReturn(1);
        when(personalRegistrationMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return List.of(p1, p2, p3).stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        });

        PersonalFormationMoveReq req = new PersonalFormationMoveReq();
        req.setPersonalRegistrationId(1L);
        req.setFromDrawTeamId(10L);
        req.setToDrawTeamId(20L);
        formationService.moveMember(100L, req);
        verify(drawTeamMapper, atLeastOnce()).updateById(argThat(t ->
                t != null && Long.valueOf(10L).equals(t.getId()) && Long.valueOf(2L).equals(t.getCaptainUserId())));
    }

    @Test
    void test13And14_lockCreatesRosterAndNoBasketTeam() {
        BasketDivision division = personalDivision("OPEN");
        stubLockHappyPath(division, 6);
        when(rosterVersionService.createVersion(100L, 9L)).thenReturn(draftVersion());
        when(eventSnapshotService.createTemporaryEventTeam(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    BasketEventTeam et = new BasketEventTeam();
                    et.setId(ids.incrementAndGet());
                    et.setSourceTeamId(null);
                    return et;
                });
        when(rosterEntryMapper.batchInsert(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(rosterVersionMapper.updateById(any())).thenReturn(1);
        when(drawBatchMapper.casLock(anyLong(), anyLong(), anyLong())).thenReturn(1);
        when(rosterVersionService.lockVersion(201L, 9L)).thenReturn(lockedVersion());
        when(rosterVersionService.getById(201L)).thenReturn(lockedVersion());

        var result = formationService.lock(100L, 9L);
        assertEquals(201L, result.getRosterVersionId());
        verify(eventSnapshotService, times(2)).createTemporaryEventTeam(any(), any(), any(), any(), any(), any(), any());
        verify(rosterEntryMapper).batchInsert(any());
        verifyNoInteractions(structureVersionService);
    }

    @Test
    void test15_duplicateLockIdempotent() {
        BasketDivision division = personalDivision("OPEN");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        stubVersionedRule();
        stubLockHappyPath(division, 6);
        BasketDivisionRosterVersion current = lockedVersion();
        current.setAllocationFingerprint("same");
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(current);
        PersonalFormationCommandServiceImpl spyService = formationService;
        ReflectionTestUtils.setField(spyService, "formationStrategyFactory", new FormationStrategyFactory(List.of(new BalancedRandomFormationStrategy())));
        // fingerprint will not match "same" unless we stub lock validate then compare
        // Force equal fingerprint by locking twice with same mocked fingerprint via current match after first lock
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null, current);
        when(rosterVersionService.createVersion(100L, 9L)).thenReturn(draftVersion());
        when(eventSnapshotService.createTemporaryEventTeam(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> eventTeam(ids.incrementAndGet()));
        when(rosterEntryMapper.batchInsert(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(rosterVersionMapper.updateById(any())).thenAnswer(inv -> {
            BasketDivisionRosterVersion v = inv.getArgument(0);
            current.setAllocationFingerprint(v.getAllocationFingerprint());
            return 1;
        });
        when(drawBatchMapper.casLock(anyLong(), anyLong(), anyLong())).thenReturn(1);
        when(rosterVersionService.lockVersion(201L, 9L)).thenReturn(current);
        when(rosterVersionService.getById(201L)).thenReturn(current);

        var first = formationService.lock(100L, 9L);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(current);
        var second = formationService.lock(100L, 9L);
        assertTrue(Boolean.TRUE.equals(second.getIdempotentReplay()));
        verify(rosterVersionService, times(1)).createVersion(anyLong(), anyLong());
    }

    @Test
    void test16_lockedFormationCannotAdjust() {
        BasketDivision division = personalDivision("OPEN");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        BasketPersonalDrawBatch batch = editableBatch();
        batch.setBatchStatus(DrawBatchStatusEnum.LOCKED.getCode());
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(batch);
        PersonalFormationMoveReq req = new PersonalFormationMoveReq();
        req.setPersonalRegistrationId(1L);
        req.setToDrawTeamId(20L);
        assertThrows(ServiceException.class, () -> formationService.moveMember(100L, req));
    }

    @Test
    void test17_versionedWithoutRosterRejectsStructure() {
        RuleVersionView rule = versionedRule();
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        when(divisionMapper.selectById(100L)).thenReturn(personalDivision("OPEN"));
        ServiceException ex = assertThrows(ServiceException.class, () -> rosterSupport.selectFormalEventTeams(100L));
        assertTrue(ex.getMessage().contains("\u4e34\u65f6\u7403\u961f") || ex.getMessage().contains("\u540d\u5355"));
    }

    @Test
    void test18_legacyWithoutRosterStillReadsActive() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.LEGACY.getCode());
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketEventTeam legacy = new BasketEventTeam();
        legacy.setId(88L);
        when(eventTeamMapper.selectActiveByDivisionId(100L)).thenReturn(List.of(legacy));
        List<BasketEventTeam> teams = rosterSupport.selectFormalEventTeams(100L);
        assertEquals(1, teams.size());
    }

    @Test
    void test19_personalRosterV2MarksOldStructureStale() {
        BasketDivision division = personalDivision("OPEN");
        stubLockHappyPath(division, 6);
        BasketDivisionRosterVersion v1 = lockedVersion();
        v1.setId(201L);
        v1.setAllocationFingerprint("old");
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(v1);
        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(List.of());
        BasketDivisionRosterVersion v2 = draftVersion();
        v2.setId(202L);
        when(rosterVersionService.createVersion(100L, 9L)).thenReturn(v2);
        when(eventSnapshotService.createTemporaryEventTeam(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> eventTeam(ids.incrementAndGet()));
        when(rosterEntryMapper.batchInsert(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(rosterVersionMapper.updateById(any())).thenReturn(1);
        when(drawBatchMapper.casLock(anyLong(), anyLong(), anyLong())).thenReturn(1);
        when(rosterVersionService.lockVersion(202L, 9L)).thenReturn(v2);
        when(rosterVersionService.getById(202L)).thenReturn(v2);
        formationService.lock(100L, 9L);
        verify(structureVersionService).invalidateByRosterVersion(201L);
    }

    @Test
    void test20_finishedMatchRejectsNewFormation() {
        BasketDivision division = personalDivision("OPEN");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        stubVersionedRule();
        BasketDivisionRosterVersion current = lockedVersion();
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(current);
        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(List.of(new BasketV4Match()));
        assertThrows(ServiceException.class, () -> formationService.generate(100L, 9L));
    }

    private void stubGenerate(BasketDivision division, int n, GameModeEnum mode) {
        division.setGameMode(mode.getCode());
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        stubVersionedRule();
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(List.of());
        List<BasketPersonalRegistration> approved = new ArrayList<>();
        List<BasketMemberAccount> accounts = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            approved.add(approvedPerson((long) i, (long) i));
            accounts.add(account((long) i, new Date(i)));
        }
        when(personalRegistrationMapper.selectApprovedByDivisionId(100L)).thenReturn(approved);
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(null);
        when(drawBatchMapper.selectNextBatchVersion(100L)).thenReturn(1);
        when(drawBatchMapper.insert(any())).thenAnswer(inv -> {
            BasketPersonalDrawBatch b = inv.getArgument(0);
            b.setId(500L);
            return 1;
        });
        when(drawTeamMapper.insert(any())).thenAnswer(inv -> {
            BasketPersonalDrawTeam t = inv.getArgument(0);
            t.setId(ids.incrementAndGet());
            return 1;
        });
        when(drawAssignmentMapper.insert(any())).thenReturn(1);
        when(drawAssignmentMapper.selectByTeamId(anyLong())).thenReturn(List.of(assignment(1L, 1L, 1L)));
        when(drawTeamMapper.selectById(anyLong())).thenAnswer(inv -> team(inv.getArgument(0), 500L));
        when(drawTeamMapper.updateById(any())).thenReturn(1);
        when(drawBatchMapper.updateById(any())).thenReturn(1);
        when(memberAccountMapper.selectByIds(any())).thenReturn(accounts);
        when(personalRegistrationMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return approved.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(approved.get(0));
        });
    }

    private void stubLockHappyPath(BasketDivision division, int n) {
        division.setGameMode(GameModeEnum.THREE_V_THREE.getCode());
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        stubVersionedRule();
        BasketPersonalDrawBatch batch = editableBatch();
        when(drawBatchMapper.selectActiveByDivision(100L)).thenReturn(batch);
        List<BasketPersonalRegistration> approved = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            approved.add(approvedPerson((long) i, (long) i));
        }
        when(personalRegistrationMapper.selectApprovedByDivisionId(100L)).thenReturn(approved);
        when(memberAccountMapper.selectByIds(any())).thenReturn(accounts(approved));
        when(personalRegistrationMapper.selectById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0);
            return approved.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);
        });
        BasketPersonalDrawTeam t1 = team(10L, batch.getId());
        t1.setTemporaryTeamName("\u4e34\u65f6\u961f\u4f0d 01");
        t1.setCaptainPersonalRegistrationId(1L);
        t1.setCaptainPlayerId(1L);
        t1.setCaptainUserId(1L);
        BasketPersonalDrawTeam t2 = team(20L, batch.getId());
        t2.setTemporaryTeamName("\u4e34\u65f6\u961f\u4f0d 02");
        t2.setCaptainPersonalRegistrationId(4L);
        t2.setCaptainPlayerId(4L);
        t2.setCaptainUserId(4L);
        when(drawTeamMapper.selectByBatchId(batch.getId())).thenReturn(List.of(t1, t2));
        List<BasketPersonalDrawAssignment> a1 = List.of(assignment(1L, 10L, 1L), assignment(2L, 10L, 2L), assignment(3L, 10L, 3L));
        List<BasketPersonalDrawAssignment> a2 = List.of(assignment(4L, 20L, 4L), assignment(5L, 20L, 5L), assignment(6L, 20L, 6L));
        when(drawAssignmentMapper.selectByBatchId(batch.getId())).thenReturn(concat(a1, a2));
        when(drawAssignmentMapper.selectByTeamId(10L)).thenReturn(a1);
        when(drawAssignmentMapper.selectByTeamId(20L)).thenReturn(a2);
        when(drawTeamMapper.selectById(10L)).thenReturn(t1);
        when(drawTeamMapper.selectById(20L)).thenReturn(t2);
        when(drawTeamMapper.updateById(any())).thenReturn(1);
        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(List.of());
    }

    private void stubVersionedRule() {
        when(ruleVersionService.getCurrent(100L)).thenReturn(versionedRule());
    }

    private RuleVersionView versionedRule() {
        RuleVersionView rule = new RuleVersionView();
        rule.setRuleVersionId(11L);
        rule.setLocked(true);
        rule.setRuleVersionStatus("LOCKED");
        rule.setRosterWorkflowMode(RosterWorkflowModeEnum.VERSIONED.getCode());
        return rule;
    }

    private BasketDivision personalDivision(String seriesHint) {
        BasketDivision division = new BasketDivision();
        division.setId(100L);
        division.setSeasonId(8L);
        division.setDivisionName(seriesHint);
        division.setRegistrationMode(RegistrationModeEnum.PERSONAL.getCode());
        division.setGameMode(GameModeEnum.THREE_V_THREE.getCode());
        division.setPlatformCode("PC");
        return division;
    }

    private BasketPersonalDrawBatch editableBatch() {
        BasketPersonalDrawBatch batch = new BasketPersonalDrawBatch();
        batch.setId(500L);
        batch.setDivisionId(100L);
        batch.setBatchStatus(DrawBatchStatusEnum.GENERATED.getCode());
        return batch;
    }

    private BasketPersonalDrawTeam team(Long id, Long batchId) {
        BasketPersonalDrawTeam team = new BasketPersonalDrawTeam();
        team.setId(id);
        team.setDrawBatchId(batchId);
        team.setDivisionId(100L);
        team.setTemporaryTeamCode("T" + id);
        team.setTemporaryTeamName("T" + id);
        return team;
    }

    private BasketPersonalDrawAssignment assignment(Long personalId, Long teamId, Long playerId) {
        BasketPersonalDrawAssignment a = new BasketPersonalDrawAssignment();
        a.setId(personalId);
        a.setDrawBatchId(500L);
        a.setDrawTeamId(teamId);
        a.setPersonalRegistrationId(personalId);
        a.setPlayerId(playerId);
        a.setAssignmentRole(AssignmentRoleEnum.MEMBER.getCode());
        return a;
    }

    private BasketPersonalRegistration approvedPerson(Long id, Long userId) {
        BasketPersonalRegistration row = new BasketPersonalRegistration();
        row.setId(id);
        row.setDivisionId(100L);
        row.setPlayerId(id);
        row.setUserId(userId);
        row.setRegistrationStatus(PersonalRegistrationStatusEnum.APPROVED.getCode());
        return row;
    }

    private BasketMemberAccount account(Long id, Date createTime) {
        BasketMemberAccount acc = new BasketMemberAccount();
        acc.setId(id);
        acc.setCreateTime(createTime);
        return acc;
    }

    private List<BasketMemberAccount> accounts(List<BasketPersonalRegistration> approved) {
        List<BasketMemberAccount> rows = new ArrayList<>();
        for (int i = 0; i < approved.size(); i++) {
            rows.add(account(approved.get(i).getUserId(), new Date(i + 1)));
        }
        return rows;
    }

    private BasketDivisionRosterVersion draftVersion() {
        BasketDivisionRosterVersion v = new BasketDivisionRosterVersion();
        v.setId(201L);
        v.setDivisionId(100L);
        v.setVersionNo(1);
        v.setStatus(RosterVersionStatusEnum.DRAFT.getCode());
        return v;
    }

    private BasketDivisionRosterVersion lockedVersion() {
        BasketDivisionRosterVersion v = draftVersion();
        v.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        v.setIsCurrent(1);
        return v;
    }

    private BasketEventTeam eventTeam(Long id) {
        BasketEventTeam et = new BasketEventTeam();
        et.setId(id);
        et.setSourceTeamId(null);
        return et;
    }

    private com.zhs.basket.support.v4.formation.FormationPlayer player(Long personalId, Long userId) {
        com.zhs.basket.support.v4.formation.FormationPlayer p = new com.zhs.basket.support.v4.formation.FormationPlayer();
        p.setPersonalRegistrationId(personalId);
        p.setPlayerId(personalId);
        p.setUserId(userId);
        return p;
    }

    private List<BasketPersonalDrawAssignment> concat(List<BasketPersonalDrawAssignment> a, List<BasketPersonalDrawAssignment> b) {
        List<BasketPersonalDrawAssignment> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }
}
