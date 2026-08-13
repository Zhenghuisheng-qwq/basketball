package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterEntry;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketV4Match;
import com.zhs.basket.dto.v4.TeamRosterAllocationReq;
import com.zhs.basket.dto.v4.TeamRosterCandidateOrderReq;
import com.zhs.basket.dto.v4.TeamRosterPromoteReq;
import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RegistrationSourceEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.enums.v4.RosterVersionStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterEntryMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.service.v4.IBasketV4EventSnapshotService;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.service.v4.IBasketV4RuleVersionService;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.roster.AutoCapacityAllocationStrategy;
import com.zhs.basket.support.v4.roster.RosterAllocationStrategyFactory;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamRosterR2ServiceTest {

    @Mock private BasketRegistrationMapper registrationMapper;
    @Mock private BasketTeamMapper teamMapper;
    @Mock private BasketDivisionRosterEntryMapper rosterEntryMapper;
    @Mock private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Mock private IBasketV4RosterVersionService rosterVersionService;
    @Mock private IBasketV4RuleVersionService ruleVersionService;
    @Mock private IBasketV4EventSnapshotService eventSnapshotService;
    @Mock private IBasketV4StructureVersionService structureVersionService;
    @Mock private BasketBusinessAuditWriter auditWriter;
    @Mock private BasketV4MatchMapper matchMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketDivisionMapper divisionMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketEventTeamMapper eventTeamMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper registrationAuditMapper;
    @Mock private com.zhs.basket.mapper.v4.BasketCaptainGrantMapper captainGrantMapper;
    @Mock private com.zhs.basket.support.v4.TeamRegistrationEligibilitySpecification eligibilitySpecification;
    @Mock private com.zhs.basket.support.v4.CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Mock private com.zhs.basket.support.BasketAdminMessageService messageService;

    @InjectMocks private TeamRosterAllocationServiceImpl allocationService;
    @InjectMocks private TeamRosterLockApplicationServiceImpl lockService;
    @InjectMocks private TeamRosterCandidatePromotionServiceImpl promotionService;
    @InjectMocks private BasketV4RegistrationAuditServiceImpl auditService;

    private TeamRosterSupport rosterSupport;
    private BasketDivision division;

    @BeforeEach
    void setUp() {
        rosterSupport = new TeamRosterSupport();
        ReflectionTestUtils.setField(rosterSupport, "divisionMapper", divisionMapper);
        ReflectionTestUtils.setField(rosterSupport, "eventTeamMapper", eventTeamMapper);
        ReflectionTestUtils.setField(rosterSupport, "rosterVersionService", rosterVersionService);
        ReflectionTestUtils.setField(rosterSupport, "ruleVersionService", ruleVersionService);

        RosterAllocationStrategyFactory factory = new RosterAllocationStrategyFactory(List.of(new AutoCapacityAllocationStrategy()));
        ReflectionTestUtils.setField(allocationService, "rosterSupport", rosterSupport);
        ReflectionTestUtils.setField(allocationService, "strategyFactory", factory);
        ReflectionTestUtils.setField(lockService, "rosterSupport", rosterSupport);
        ReflectionTestUtils.setField(promotionService, "rosterSupport", rosterSupport);
        ReflectionTestUtils.setField(promotionService, "lockApplicationService", lockService);
        ReflectionTestUtils.setField(auditService, "teamRosterSupport", rosterSupport);

        division = new BasketDivision();
        division.setId(100L);
        division.setDivisionName("R2");
        division.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        division.setMaxTeamCount(4);
    }

    @Test
    void test1_approvedDoesNotCreateEventTeam() {
        BasketRegistration registration = approved(1L, ParticipationRoleEnum.UNALLOCATED);
        registration.setRegistrationStatus(RegistrationStatusEnum.PENDING_REVIEW.getCode());
        registration.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        registration.setRegistrationSource(RegistrationSourceEnum.TEAM_DIRECT.getCode());
        when(registrationMapper.selectById(1L)).thenReturn(registration);
        when(divisionMapper.selectById(100L)).thenReturn(division);
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(List.of(new com.zhs.basket.domain.v4.BasketRegistrationPlayer()));
        when(registrationMapper.updateById(any())).thenReturn(1);
        try (var mocked = org.mockito.Mockito.mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getUserId).thenReturn(9L);
            mocked.when(SecurityUtils::getUsername).thenReturn("admin");
            auditService.approve(1L);
        }
        verify(eventSnapshotService, never()).generateFromApprovedRegistration(any());
    }

    @Test
    void test2_rejectedCannotFormal() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketRegistration rejected = new BasketRegistration();
        rejected.setId(2L);
        rejected.setDivisionId(100L);
        rejected.setRegistrationStatus(RegistrationStatusEnum.REJECTED.getCode());
        rejected.setParticipationRole(ParticipationRoleEnum.UNALLOCATED.getCode());
        when(registrationMapper.selectById(2L)).thenReturn(rejected);
        TeamRosterAllocationReq req = new TeamRosterAllocationReq();
        req.setRegistrationId(2L);
        req.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
        assertThrows(ServiceException.class, () -> allocationService.assign(100L, req, 9L));
    }

    @Test
    void test3_formalCapRejectsFifth() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketRegistration fifth = approved(5L, ParticipationRoleEnum.UNALLOCATED);
        when(registrationMapper.selectById(5L)).thenReturn(fifth);
        when(registrationMapper.countApprovedByDivisionAndRole(100L, ParticipationRoleEnum.FORMAL.getCode())).thenReturn(4);
        TeamRosterAllocationReq req = new TeamRosterAllocationReq();
        req.setRegistrationId(5L);
        req.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
        assertThrows(ServiceException.class, () -> allocationService.assign(100L, req, 9L));
    }

    @Test
    void test4_concurrentFormalCap() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        AtomicInteger formal = new AtomicInteger(3);
        when(registrationMapper.countApprovedByDivisionAndRole(100L, ParticipationRoleEnum.FORMAL.getCode()))
                .thenAnswer(inv -> formal.get());
        when(registrationMapper.updateParticipation(any())).thenAnswer(inv -> {
            BasketRegistration row = inv.getArgument(0);
            if (ParticipationRoleEnum.FORMAL.getCode().equals(row.getParticipationRole())) {
                formal.incrementAndGet();
            }
            return 1;
        });
        BasketRegistration a = approved(11L, ParticipationRoleEnum.UNALLOCATED);
        BasketRegistration b = approved(12L, ParticipationRoleEnum.UNALLOCATED);
        when(registrationMapper.selectById(11L)).thenReturn(a);
        when(registrationMapper.selectById(12L)).thenReturn(b);
        TeamRosterAllocationReq reqA = req(11L, ParticipationRoleEnum.FORMAL);
        TeamRosterAllocationReq reqB = req(12L, ParticipationRoleEnum.FORMAL);
        allocationService.assign(100L, reqA, 9L);
        assertThrows(ServiceException.class, () -> allocationService.assign(100L, reqB, 9L));
        assertTrue(formal.get() <= 4);
    }

    @Test
    void test5_autoAllocate() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        List<BasketRegistration> rows = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            BasketRegistration row = approved((long) i, ParticipationRoleEnum.UNALLOCATED);
            row.setSubmitTime(new Date(i));
            rows.add(row);
            when(registrationMapper.selectById((long) i)).thenReturn(row);
        }
        when(registrationMapper.selectByDivisionId(100L)).thenReturn(rows);
        when(registrationMapper.updateParticipation(any())).thenReturn(1);
        allocationService.autoAllocate(100L, 9L);
        assertEquals(ParticipationRoleEnum.FORMAL.getCode(), rows.get(0).getParticipationRole());
        assertEquals(ParticipationRoleEnum.FORMAL.getCode(), rows.get(3).getParticipationRole());
        assertEquals(ParticipationRoleEnum.CANDIDATE.getCode(), rows.get(4).getParticipationRole());
        assertEquals(1, rows.get(4).getCandidateOrder());
        assertEquals(2, rows.get(5).getCandidateOrder());
    }

    @Test
    void test6_candidateMoveUp() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketRegistration a = approved(1L, ParticipationRoleEnum.CANDIDATE);
        a.setCandidateOrder(1);
        BasketRegistration b = approved(2L, ParticipationRoleEnum.CANDIDATE);
        b.setCandidateOrder(2);
        when(registrationMapper.selectById(2L)).thenReturn(b);
        when(registrationMapper.selectByDivisionId(100L)).thenReturn(List.of(a, b));
        when(registrationMapper.updateParticipation(any())).thenReturn(1);
        TeamRosterCandidateOrderReq req = new TeamRosterCandidateOrderReq();
        req.setRegistrationId(2L);
        req.setDirection("UP");
        allocationService.moveCandidate(100L, req, 9L);
        assertEquals(1, b.getCandidateOrder());
        assertEquals(2, a.getCandidateOrder());
    }

    @Test
    void test7and8_lockAndReadonly() {
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        RuleVersionView rule = new RuleVersionView();
        rule.setLocked(true);
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        List<BasketRegistration> rows = List.of(
                approved(1L, ParticipationRoleEnum.FORMAL),
                approved(2L, ParticipationRoleEnum.FORMAL),
                candidate(3L, 1)
        );
        when(registrationMapper.selectByDivisionId(100L)).thenReturn(rows);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketDivisionRosterVersion created = draftVersion();
        when(rosterVersionService.createVersion(100L, 9L)).thenReturn(created);
        when(rosterEntryMapper.batchInsert(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(rosterVersionMapper.updateById(any())).thenReturn(1);
        BasketDivisionRosterVersion locked = draftVersion();
        locked.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        when(rosterVersionService.lockVersion(201L, 9L)).thenReturn(locked);
        when(rosterVersionService.getById(201L)).thenReturn(locked);

        var result = lockService.lock(100L, 9L);
        assertEquals(201L, result.getRosterVersionId());
        verify(eventSnapshotService).generateFromFormalRegistration(rows.get(0), 201L);
        verify(eventSnapshotService).generateFromFormalRegistration(rows.get(1), 201L);
        verify(eventSnapshotService, never()).generateFromFormalRegistration(eq(rows.get(2)), anyLong());

        BasketDivisionRosterVersion current = new BasketDivisionRosterVersion();
        current.setId(201L);
        current.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(current);
        TeamRosterAllocationReq req = req(3L, ParticipationRoleEnum.FORMAL);
        assertThrows(ServiceException.class, () -> allocationService.assign(100L, req, 9L));
    }

    @Test
    void test11_12_13_promoteHistoryAndStaleAndResultGuard() {
        BasketDivisionRosterVersion v1 = new BasketDivisionRosterVersion();
        v1.setId(201L);
        v1.setDivisionId(100L);
        v1.setVersionNo(1);
        v1.setStatus(RosterVersionStatusEnum.LOCKED.getCode());
        v1.setAllocationFingerprint("old");
        when(divisionMapper.selectByIdForUpdate(100L)).thenReturn(division);
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(v1);
        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(List.of(new BasketV4Match()));
        TeamRosterPromoteReq req = new TeamRosterPromoteReq();
        req.setFormalRegistrationId(3L);
        req.setCandidateRegistrationId(5L);
        req.setReason("cannot play");
        assertThrows(ServiceException.class, () -> promotionService.promote(100L, req, 9L));

        when(matchMapper.selectFinishedPublishedByDivisionId(100L)).thenReturn(Collections.emptyList());
        BasketRegistration formal = approved(3L, ParticipationRoleEnum.FORMAL);
        BasketRegistration candidate = candidate(5L, 1);
        BasketRegistration keepA = approved(1L, ParticipationRoleEnum.FORMAL);
        BasketRegistration keepB = approved(2L, ParticipationRoleEnum.FORMAL);
        when(registrationMapper.selectById(3L)).thenReturn(formal);
        when(registrationMapper.selectById(5L)).thenReturn(candidate);
        when(registrationMapper.updateParticipation(any())).thenReturn(1);
        List<BasketRegistration> after = new ArrayList<>();
        after.add(keepA);
        after.add(keepB);
        after.add(formal);
        after.add(candidate);
        when(registrationMapper.selectByDivisionId(100L)).thenAnswer(inv -> {
            formal.setParticipationRole(ParticipationRoleEnum.EXCLUDED.getCode());
            candidate.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
            return after;
        });
        RuleVersionView rule = new RuleVersionView();
        rule.setLocked(true);
        when(ruleVersionService.getCurrent(100L)).thenReturn(rule);
        BasketDivisionRosterVersion v2 = new BasketDivisionRosterVersion();
        v2.setId(202L);
        v2.setDivisionId(100L);
        v2.setVersionNo(2);
        v2.setStatus(RosterVersionStatusEnum.DRAFT.getCode());
        when(rosterVersionService.createVersion(100L, 9L)).thenReturn(v2);
        when(rosterEntryMapper.batchInsert(any())).thenAnswer(inv -> ((List<?>) inv.getArgument(0)).size());
        when(rosterVersionMapper.updateById(any())).thenReturn(1);
        when(rosterVersionService.lockVersion(202L, 9L)).thenReturn(v2);
        when(rosterVersionService.getById(202L)).thenReturn(v2);

        var result = promotionService.promote(100L, req, 9L);
        assertEquals(202L, result.getRosterVersionId());
        verify(structureVersionService).invalidateByRosterVersion(201L);

        BasketDivisionRosterEntry v1c = new BasketDivisionRosterEntry();
        v1c.setRegistrationId(3L);
        v1c.setTeamId(30L);
        v1c.setParticipationRole(ParticipationRoleEnum.FORMAL.getCode());
        when(rosterVersionService.getById(201L)).thenReturn(v1);
        when(rosterEntryMapper.selectByRosterVersionId(201L)).thenReturn(List.of(v1c));
        when(registrationMapper.selectById(3L)).thenReturn(formal);
        var history = lockService.getVersion(100L, 201L);
        assertEquals(1, history.getFormalList().size());
        assertEquals(3L, history.getFormalList().get(0).getRegistrationId());
    }

    @Test
    void test14_legacyEventTeamsReadableWithoutRoster() {
        when(rosterVersionService.getCurrentLockedVersion(100L)).thenReturn(null);
        BasketEventTeam legacy = new BasketEventTeam();
        legacy.setId(88L);
        when(eventTeamMapper.selectActiveByDivisionId(100L)).thenReturn(List.of(legacy));
        List<BasketEventTeam> teams = rosterSupport.selectFormalEventTeams(100L);
        assertEquals(1, teams.size());
        assertEquals(88L, teams.get(0).getId());
    }

    private TeamRosterAllocationReq req(Long id, ParticipationRoleEnum role) {
        TeamRosterAllocationReq req = new TeamRosterAllocationReq();
        req.setRegistrationId(id);
        req.setParticipationRole(role.getCode());
        return req;
    }

    private BasketRegistration approved(Long id, ParticipationRoleEnum role) {
        BasketRegistration row = new BasketRegistration();
        row.setId(id);
        row.setDivisionId(100L);
        row.setTeamId(id * 10);
        row.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        row.setParticipationRole(role.getCode());
        row.setSubmitTime(new Date(id));
        return row;
    }

    private BasketRegistration candidate(Long id, int order) {
        BasketRegistration row = approved(id, ParticipationRoleEnum.CANDIDATE);
        row.setCandidateOrder(order);
        return row;
    }

    private BasketDivisionRosterVersion draftVersion() {
        BasketDivisionRosterVersion version = new BasketDivisionRosterVersion();
        version.setId(201L);
        version.setDivisionId(100L);
        version.setVersionNo(1);
        version.setStatus(RosterVersionStatusEnum.DRAFT.getCode());
        return version;
    }
}
