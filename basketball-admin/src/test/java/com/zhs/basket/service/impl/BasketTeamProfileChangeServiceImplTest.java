package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.BasketTeamProfileChange;
import com.zhs.basket.enums.TeamAuditStatusEnum;
import com.zhs.basket.enums.TeamProfileChangeAuditStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamProfileChangeMapper;
import com.zhs.basket.support.BasketAdminMessageService;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.web.controller.basket.BasketTeamProfileChangeController;
import org.apache.ibatis.exceptions.PersistenceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketTeamProfileChangeServiceImplTest {

    private static final Long CHANGE_ID = 1L;
    private static final Long TEAM_ID = 100L;
    private static final Long REVIEWER_ID = 9L;

    @InjectMocks
    private BasketTeamProfileChangeServiceImpl service;

    @Mock
    private BasketTeamProfileChangeMapper profileChangeMapper;
    @Mock
    private BasketTeamMapper teamMapper;
    @Mock
    private BasketAdminMessageService messageService;

    @Test
    void t01_nameOnlyKeepsShortNameAndOtherFields() {
        stubPendingChange(change("新队名", "JK", 8L, "旧介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(TEAM_ID, "新队名", "JK", 8L, "旧介绍", 1);
    }

    @Test
    void t02_descriptionOnlyKeepsNameAndShortName() {
        stubPendingChange(change("旧队名", "JK", 8L, "新介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(TEAM_ID, "旧队名", "JK", 8L, "新介绍", 1);
    }

    @Test
    void t03_legalShortNameIsApplied() {
        stubPendingChange(change("旧队名", "新简称", 8L, "旧介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(TEAM_ID, "旧队名", "新简称", 8L, "旧介绍", 1);
    }

    @Test
    void t04_nameShortNameAndDescriptionUpdateTogether() {
        stubPendingChange(change("新队名", "新简称", 8L, "新介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(TEAM_ID, "新队名", "新简称", 8L, "新介绍", 1);
        verify(profileChangeMapper).updateStatusIfMatch(eq(CHANGE_ID), anyList(),
                eq(TeamProfileChangeAuditStatusEnum.APPROVED.getCode()), eq(REVIEWER_ID),
                isNull(), any(), eq(0));
        verify(messageService).sendSiteMessage(any(), eq(REVIEWER_ID), eq("TEAM_PROFILE_CHANGE"), eq(CHANGE_ID),
                any(), any(), any(), any());
    }

    @Test
    void t05_logoUpdateKeepsShortName() {
        stubPendingChange(change("旧队名", "JK", 88L, "旧介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(TEAM_ID, "旧队名", "JK", 88L, "旧介绍", 1);
    }

    @Test
    void t06_nullableDescriptionCanBeClearedWithoutClearingShortName() {
        BasketTeamProfileChange change = change("旧队名", "JK", null, null, 1);
        stubPendingChange(change);
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        verify(teamMapper).updateProfileFromChange(eq(TEAM_ID), eq("旧队名"), eq("JK"), isNull(), isNull(), eq(1));
    }

    @Test
    void t07_missingShortNameIsRejectedBeforeTeamUpdate() {
        stubPendingChange(change("新队名", null, 8L, "新介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("新球队简称不能为空", ex.getMessage());
        assertFalse(ex.getMessage().contains("short_name"));
        verify(teamMapper, never()).updateProfileFromChange(any(), any(), any(), any(), any(), any());
        verify(profileChangeMapper, never()).updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void t08_legacyChangeWithoutShortNameDoesNotOverwriteOfficialTeam() {
        BasketTeamProfileChange change = change("新队名", "   ", 8L, "新介绍", 1);
        change.setOldTeamShortName("JK");
        stubPendingChange(change);
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("新球队简称不能为空", ex.getMessage());
        verify(teamMapper, never()).updateProfileFromChange(any(), any(), any(), any(), any(), any());
        verify(profileChangeMapper, never()).updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void t09_teamUpdateFailureDoesNotMarkChangeApproved() {
        stubPendingChange(change("新队名", "JK", 8L, "旧介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        when(teamMapper.updateProfileFromChange(anyLong(), any(), any(), any(), any(), anyInt()))
                .thenThrow(new PersistenceException("### Error updating database. Cause: Column 'short_name' cannot be null ### SQL: update basket_team set short_name = ?"));

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("更新正式球队资料失败", ex.getMessage());
        assertFalse(ex.getMessage().toLowerCase().contains("sql"));
        assertFalse(ex.getMessage().contains("short_name"));
        verify(profileChangeMapper, never()).updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any());
        verify(messageService, never()).sendSiteMessage(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void t10_repeatApproveIsRejected() {
        BasketTeamProfileChange change = change("新队名", "JK", 8L, "介绍", 1);
        change.setAuditStatus(TeamProfileChangeAuditStatusEnum.APPROVED.getCode());
        stubPendingChange(change);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("申请状态不可审核", ex.getMessage());
        verify(teamMapper, never()).selectById(any());
        verify(teamMapper, never()).updateProfileFromChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void t11_lostStatusRaceDoesNotSendApprovalMessage() {
        stubPendingChange(change("新队名", "JK", 8L, "介绍", 1));
        stubApprovedTeam("旧队名", "JK", 8L, "介绍", 1);
        stubUniqueName();
        when(teamMapper.updateProfileFromChange(anyLong(), any(), any(), any(), any(), anyInt())).thenReturn(1);
        when(profileChangeMapper.updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any())).thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, this::approve);

        assertEquals("审核通过失败，状态已变更", ex.getMessage());
        verify(messageService, never()).sendSiteMessage(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void t12_rejectedChangeCannotBeApproved() {
        BasketTeamProfileChange change = change("新队名", "JK", 8L, "介绍", 1);
        change.setAuditStatus(TeamProfileChangeAuditStatusEnum.REJECTED.getCode());
        stubPendingChange(change);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("申请状态不可审核", ex.getMessage());
        verify(teamMapper, never()).updateProfileFromChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    void t13_approveRequiresAuditPermission() throws Exception {
        Method method = BasketTeamProfileChangeController.class.getMethod(
                "approve", Long.class, com.zhs.basket.req.TeamProfileChangeApproveReq.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("basket:teamProfileChange:audit"));
    }

    @Test
    void t14_missingTeamDoesNotChangeAuditStatus() {
        stubPendingChange(change("新队名", "JK", 8L, "介绍", 1));
        when(teamMapper.selectById(TEAM_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(CHANGE_ID, null));

        assertEquals("球队不存在", ex.getMessage());
        verify(teamMapper, never()).updateProfileFromChange(any(), any(), any(), any(), any(), any());
        verify(profileChangeMapper, never()).updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void t15_unchangedShortNameIsWrittenBackAsOriginalValue() {
        stubPendingChange(change("新队名", " JK ", 8L, "新介绍", null));
        stubApprovedTeam("旧队名", "JK", 8L, "旧介绍", 1);
        stubUniqueName();
        stubProfileAndStatusUpdated();

        approve();

        ArgumentCaptor<String> shortName = ArgumentCaptor.forClass(String.class);
        verify(teamMapper).updateProfileFromChange(eq(TEAM_ID), eq("新队名"), shortName.capture(), eq(8L), eq("新介绍"), eq(1));
        assertEquals("JK", shortName.getValue());
    }

    private void approve() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(REVIEWER_ID);
            service.approve(CHANGE_ID, null);
        }
    }

    private void stubPendingChange(BasketTeamProfileChange change) {
        when(profileChangeMapper.selectById(CHANGE_ID)).thenReturn(change);
    }

    private void stubApprovedTeam(String teamName, String shortName, Long logoFileId, String description, Integer publicStatus) {
        BasketTeam team = new BasketTeam();
        team.setId(TEAM_ID);
        team.setDivisionId(7L);
        team.setTeamName(teamName);
        team.setShortName(shortName);
        team.setLogoFileId(logoFileId);
        team.setDescription(description);
        team.setPublicStatus(publicStatus);
        team.setAuditStatus(TeamAuditStatusEnum.APPROVED.getCode());
        when(teamMapper.selectById(TEAM_ID)).thenReturn(team);
    }

    private void stubUniqueName() {
        when(teamMapper.countByTeamNameExclude(anyLong(), any(), anyLong())).thenReturn(0);
    }

    private void stubProfileAndStatusUpdated() {
        when(teamMapper.updateProfileFromChange(anyLong(), any(), any(), any(), any(), anyInt())).thenReturn(1);
        when(profileChangeMapper.updateStatusIfMatch(any(), any(), any(), any(), any(), any(), any())).thenReturn(1);
    }

    private BasketTeamProfileChange change(String teamName, String shortName, Long logoFileId,
                                           String description, Integer publicStatus) {
        BasketTeamProfileChange change = new BasketTeamProfileChange();
        change.setId(CHANGE_ID);
        change.setTeamId(TEAM_ID);
        change.setSubmitUserId(3L);
        change.setAuditStatus(TeamProfileChangeAuditStatusEnum.PENDING.getCode());
        change.setActiveFlag(1);
        change.setOldTeamName("旧队名");
        change.setNewTeamName(teamName);
        change.setOldTeamShortName("JK");
        change.setNewTeamShortName(shortName);
        change.setNewLogoFileId(logoFileId);
        change.setNewDescription(description);
        change.setNewPublicStatus(publicStatus);
        return change;
    }
}
