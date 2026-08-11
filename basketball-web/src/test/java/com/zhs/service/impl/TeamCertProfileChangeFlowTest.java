package com.zhs.service.impl;

import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketTeamAuditRecordDao;
import com.zhs.dao.BasketTeamDao;
import com.zhs.dao.BasketTeamMemberDao;
import com.zhs.dao.BasketTeamProfileChangeDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.TeamAuditStatusEnum;
import com.zhs.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.enums.TeamStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTeam;
import com.zhs.model.BasketTeamAuditRecord;
import com.zhs.model.BasketTeamMember;
import com.zhs.model.BasketTeamProfileChange;
import com.zhs.request.team.TeamProfileChangeSubmitReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.support.TeamWritableSupport;
import com.zhs.util.AccountSecurityLogHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CASE 1~12：球队认证 vs 资料变更分流。
 */
@ExtendWith(MockitoExtension.class)
class TeamCertProfileChangeFlowTest {

    private static final Long MEMBER_ID = 9001L;
    private static final Long TEAM_ID = 10001L;

    @Mock private BasketTeamDao teamDao;
    @Mock private BasketTeamAuditRecordDao teamAuditRecordDao;
    @Mock private BasketTeamMemberDao teamMemberDao;
    @Mock private BasketTeamProfileChangeDao profileChangeDao;
    @Mock private BasketPlayerProfileDao playerProfileDao;
    @Mock private BasketFileAssetDao fileAssetDao;
    @Mock private ICaptainPermissionService captainPermissionService;
    @Mock private TeamWritableSupport teamWritableSupport;
    @Mock private AccountSecurityLogHelper securityLogHelper;

    @InjectMocks
    private AccountTeamServiceImpl accountTeamService;
    @InjectMocks
    private TeamProfileChangeServiceImpl profileChangeService;

    private final AtomicInteger profileChangeInserts = new AtomicInteger();

    @BeforeEach
    void setUp() {
        UserContext.setUserId(MEMBER_ID);
        profileChangeInserts.set(0);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    /** CASE 1: DRAFT 编辑资料 PASS，不产生 ProfileChange */
    @Test
    void case1_draftEditDoesNotCreateProfileChange() {
        BasketTeam team = team(TeamAuditStatusEnum.DRAFT);
        stubCaptainWritable(team);
        stubMembership();

        TeamUpdateReq req = new TeamUpdateReq();
        req.setTeamName("ZLZ");
        req.setDescription("简介 B");
        accountTeamService.update(String.valueOf(TEAM_ID), req, "127.0.0.1");

        assertEquals(TeamAuditStatusEnum.DRAFT.getCode(), team.getAuditStatus());
        assertEquals("简介 B", team.getDescription());
        verify(teamDao).updateById(team);
        verify(profileChangeDao, never()).insert(isA(BasketTeamProfileChange.class));
    }

    /** CASE 2: PENDING 禁止编辑 */
    @Test
    void case2_pendingRejectsEdit() {
        BasketTeam team = team(TeamAuditStatusEnum.PENDING);
        stubCaptainWritable(team);

        TeamUpdateReq req = new TeamUpdateReq();
        req.setDescription("should fail");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountTeamService.update(String.valueOf(TEAM_ID), req, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("认证审核中"));
        verify(teamDao, never()).updateById(isA(BasketTeam.class));
    }

    /** CASE 3: 语义 — REJECTED 状态码正确（Admin reject CAS 另测）；此处校验枚举事实 */
    @Test
    void case3_rejectedStatusCode() {
        assertEquals(2, TeamAuditStatusEnum.REJECTED.getCode());
    }

    /** CASE 4: REJECTED 改 Logo/简介 PASS，ProfileChange 不增加 */
    @Test
    void case4_rejectedEditDoesNotCreateProfileChange() {
        BasketTeam team = team(TeamAuditStatusEnum.REJECTED);
        stubCaptainWritable(team);
        stubMembership();

        TeamUpdateReq req = new TeamUpdateReq();
        req.setTeamName("ZLZ");
        req.setDescription("简介 B");
        accountTeamService.update(String.valueOf(TEAM_ID), req, "127.0.0.1");

        assertEquals(TeamAuditStatusEnum.REJECTED.getCode(), team.getAuditStatus());
        assertEquals("简介 B", team.getDescription());
        verify(teamAuditRecordDao, never()).insert(isA(BasketTeamAuditRecord.class));
        verify(profileChangeDao, never()).insert(isA(BasketTeamProfileChange.class));
    }

    /** CASE 5: REJECTED → Resubmit → PENDING，写 RESUBMIT 记录 */
    @Test
    void case5_rejectedResubmitGoesPending() {
        BasketTeam team = team(TeamAuditStatusEnum.REJECTED);
        stubCaptainWritable(team);
        stubMembership();

        accountTeamService.submitCertification(String.valueOf(TEAM_ID), "127.0.0.1");

        assertEquals(TeamAuditStatusEnum.PENDING.getCode(), team.getAuditStatus());
        ArgumentCaptor<BasketTeamAuditRecord> captor = ArgumentCaptor.forClass(BasketTeamAuditRecord.class);
        verify(teamAuditRecordDao).insert(captor.capture());
        assertEquals("RESUBMIT", captor.getValue().getAuditAction());
        assertEquals(TeamAuditStatusEnum.PENDING.getCode(), captor.getValue().getAfterStatus());
    }

    /** CASE 6: DRAFT → Submit → PENDING（首次提交，供 Admin 认证队列） */
    @Test
    void case6_draftSubmitGoesPending() {
        BasketTeam team = team(TeamAuditStatusEnum.DRAFT);
        stubCaptainWritable(team);
        stubMembership();

        accountTeamService.submitCertification(String.valueOf(TEAM_ID), "127.0.0.1");

        assertEquals(TeamAuditStatusEnum.PENDING.getCode(), team.getAuditStatus());
        ArgumentCaptor<BasketTeamAuditRecord> captor = ArgumentCaptor.forClass(BasketTeamAuditRecord.class);
        verify(teamAuditRecordDao).insert(captor.capture());
        assertEquals("SUBMIT", captor.getValue().getAuditAction());
    }

    /** CASE 7: APPROVED 编辑公开资料 → 创建 ProfileChange PENDING，不直接改正式资料 */
    @Test
    void case7_approvedCreatesProfileChange() {
        BasketTeam team = team(TeamAuditStatusEnum.APPROVED);
        team.setDescription("简介 A");
        stubCaptainForProfileChange(team);
        when(profileChangeDao.selectActiveByTeamId(TEAM_ID)).thenReturn(null);
        when(profileChangeDao.insert(any(BasketTeamProfileChange.class))).thenAnswer(inv -> {
            profileChangeInserts.incrementAndGet();
            BasketTeamProfileChange row = inv.getArgument(0);
            row.setId(555L);
            return 1;
        });

        TeamProfileChangeSubmitReq req = new TeamProfileChangeSubmitReq();
        req.setTeamName("ZLZ");
        req.setDescription("简介 B");
        profileChangeService.submitChange(String.valueOf(TEAM_ID), req);

        assertEquals(1, profileChangeInserts.get());
        assertEquals("简介 A", team.getDescription());
        verify(teamDao, never()).updateById(isA(BasketTeam.class));
    }

    /** CASE 8: ProfileChange PENDING 时第二次提交 REJECT */
    @Test
    void case8_secondProfileChangeWhilePendingRejected() {
        BasketTeam team = team(TeamAuditStatusEnum.APPROVED);
        stubCaptainForProfileChange(team);
        BasketTeamProfileChange pending = new BasketTeamProfileChange();
        pending.setId(1L);
        pending.setAuditStatus(TeamProfileChangeAuditStatusEnum.PENDING.getCode());
        pending.setActiveFlag(1);
        when(profileChangeDao.selectActiveByTeamId(TEAM_ID)).thenReturn(pending);

        TeamProfileChangeSubmitReq req = new TeamProfileChangeSubmitReq();
        req.setTeamName("ZLZ");
        req.setDescription("简介 C");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> profileChangeService.submitChange(String.valueOf(TEAM_ID), req));
        assertTrue(ex.getMessage().contains("待审核的资料变更"));
        verify(profileChangeDao, never()).insert(isA(BasketTeamProfileChange.class));
    }

    /** CASE 9/11 语义：未认证不可走 ProfileChange；已认证才可 */
    @Test
    void case9_nonApprovedCannotSubmitProfileChange() {
        BasketTeam team = team(TeamAuditStatusEnum.REJECTED);
        when(captainPermissionService.requireCertifiedPlayer(MEMBER_ID)).thenReturn(player());
        when(captainPermissionService.requireTeamCaptain(MEMBER_ID, TEAM_ID)).thenReturn(team);
        TeamProfileChangeSubmitReq req = new TeamProfileChangeSubmitReq();
        req.setTeamName("ZLZ");
        req.setDescription("简介 B");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> profileChangeService.submitChange(String.valueOf(TEAM_ID), req));
        assertTrue(ex.getMessage().contains("尚未认证"));
        verify(profileChangeDao, never()).insert(isA(BasketTeamProfileChange.class));
    }

    /** CASE 10: APPROVED 可再次提交（无 pending 时） */
    @Test
    void case10_approvedCanResubmitAfterNoPending() {
        case7_approvedCreatesProfileChange();
    }

    /** CASE 12: ARCHIVED 拒绝认证提交 / 资料变更 */
    @Test
    void case12_archivedRejectsCertificationAndProfileChange() {
        BasketTeam team = team(TeamAuditStatusEnum.DRAFT);
        team.setTeamStatus(TeamStatusEnum.ARCHIVED.getCode());
        when(captainPermissionService.requireTeamCaptain(MEMBER_ID, TEAM_ID)).thenReturn(team);
        org.mockito.Mockito.doThrow(new ServiceException("当前球队不可操作", 403))
                .when(teamWritableSupport).assertTeamWritable(team);

        assertThrows(ServiceException.class,
                () -> accountTeamService.submitCertification(String.valueOf(TEAM_ID), "127.0.0.1"));

        when(captainPermissionService.requireCertifiedPlayer(MEMBER_ID)).thenReturn(player());
        team.setAuditStatus(TeamAuditStatusEnum.APPROVED.getCode());
        assertThrows(ServiceException.class, () -> {
            TeamProfileChangeSubmitReq req = new TeamProfileChangeSubmitReq();
            req.setTeamName("ZLZ");
            req.setDescription("x");
            profileChangeService.submitChange(String.valueOf(TEAM_ID), req);
        });
    }

    @Test
    void approvedDirectNameUpdateRejected() {
        BasketTeam team = team(TeamAuditStatusEnum.APPROVED);
        stubCaptainWritable(team);
        TeamUpdateReq req = new TeamUpdateReq();
        req.setTeamName("NEW-NAME");
        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountTeamService.update(String.valueOf(TEAM_ID), req, "127.0.0.1"));
        assertTrue(ex.getMessage().contains("资料变更审核"));
    }

    @Test
    void approvedCannotResubmitCertification() {
        BasketTeam team = team(TeamAuditStatusEnum.APPROVED);
        stubCaptainWritable(team);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> accountTeamService.submitCertification(String.valueOf(TEAM_ID), "127.0.0.1"));
        assertTrue(ex.getMessage().contains("已完成认证"));
    }

    private void stubCaptainWritable(BasketTeam team) {
        when(captainPermissionService.requireTeamCaptain(MEMBER_ID, TEAM_ID)).thenReturn(team);
        doNothing().when(teamWritableSupport).assertTeamWritable(team);
    }

    private void stubMembership() {
        BasketTeamMember membership = new BasketTeamMember();
        membership.setTeamId(TEAM_ID);
        membership.setMemberId(MEMBER_ID);
        membership.setRoleType("CAPTAIN");
        membership.setIsCurrent(1);
        membership.setMembershipStatus(1);
        membership.setIsDelete(0);
        when(teamMemberDao.selectOne(any())).thenReturn(membership);
    }

    private void stubCaptainForProfileChange(BasketTeam team) {
        when(captainPermissionService.requireCertifiedPlayer(MEMBER_ID)).thenReturn(player());
        when(captainPermissionService.requireTeamCaptain(MEMBER_ID, TEAM_ID)).thenReturn(team);
        doNothing().when(teamWritableSupport).assertTeamWritable(team);
        when(teamDao.selectCount(any())).thenReturn(0L);
    }

    private BasketPlayerProfile player() {
        BasketPlayerProfile p = new BasketPlayerProfile();
        p.setId(7001L);
        p.setMemberId(MEMBER_ID);
        p.setCertificationStatus(3);
        return p;
    }

    private BasketTeam team(TeamAuditStatusEnum status) {
        BasketTeam team = new BasketTeam();
        team.setId(TEAM_ID);
        team.setTeamName("ZLZ");
        team.setShortName("ZLZ");
        team.setDescription("简介 A");
        team.setAuditStatus(status.getCode());
        team.setTeamStatus(TeamStatusEnum.ACTIVE.getCode());
        team.setDivisionId(118L);
        team.setPublicStatus(1);
        team.setIsDelete(0);
        team.setCaptainMemberId(MEMBER_ID);
        return team;
    }
}
