package com.zhs.basket.support.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.domain.BasketPlayerProfile;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketEventPlayer;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationPlayer;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChange;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChangePlayer;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationRosterChangeMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.query.v4.BasketRegistrationAuditQuery;
import com.zhs.basket.service.impl.v4.BasketV4RegistrationAuditServiceImpl;
import com.zhs.basket.service.impl.v4.BasketV4RosterChangeAuditServiceImpl;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.vo.v4.BasketRegistrationAuditDetailVo;
import com.zhs.basket.vo.v4.BasketRegistrationAuditListVo;
import com.zhs.basket.vo.v4.RosterSyncResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.framework.config.SecurityConfig;
import com.zhs.web.controller.basket.v4.BasketV4TeamRosterController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseGateBoundaryTest {

    @InjectMocks
    private BasketV4RegistrationAuditServiceImpl registrationAuditService;
    @InjectMocks
    private OpenRosterMembershipSync openRosterSync;
    @InjectMocks
    private PreMatchEventPlayerSync eventPlayerSync;
    @InjectMocks
    private BasketV4RosterChangeAuditServiceImpl rosterChangeAuditService;

    @Mock
    private BasketRegistrationMapper registrationMapper;
    @Mock
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Mock
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketTeamMemberMapper teamMemberMapper;
    @Mock
    private RosterEligibilityQueries eligibilityQueries;
    @Mock
    private OpenRosterMembershipSync openRosterMembershipSync;
    @Mock
    private BasketRegistrationRosterChangeMapper rosterChangeMapper;
    @Mock
    private IBasketV4RosterVersionService rosterVersionService;
    @Mock
    private BasketV4ParticipationSupport participationSupport;
    @Mock
    private BasketEventTeamMapper eventTeamMapper;
    @Mock
    private BasketEventPlayerMapper eventPlayerMapper;
    @Mock
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Mock
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private PreMatchEventPlayerSync eventPlayerSyncMock;

    @Test
    void approvedBelowMinimumStillOpensDetail() {
        BasketRegistrationAuditListVo row = new BasketRegistrationAuditListVo();
        row.setRegistrationId(354L);
        row.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        row.setCurrentEligibleCount(2);
        row.setLineupMinCount(5);
        when(registrationMapper.selectAuditVoList(any())).thenReturn(List.of(row));

        List<BasketRegistrationAuditListVo> listed = registrationAuditService.selectList(new BasketRegistrationAuditQuery());
        assertEquals("INELIGIBLE", listed.get(0).getEligibilityStatus());

        BasketRegistration registration = approvedRegistration();
        registration.setId(354L);
        registration.setLineupCount(2);
        when(registrationMapper.selectById(354L)).thenReturn(registration);
        when(eligibilityQueries.eligibleCount(354L, 7L)).thenReturn(2);
        when(teamMemberMapper.countCurrentMembers(8L)).thenReturn(2);
        when(registrationPlayerMapper.selectVoByRegistrationId(354L)).thenReturn(List.of());
        when(divisionMapper.selectById(7L)).thenReturn(openDivision());
        when(registrationPlayerMapper.selectCurrentMembersOutsideRoster(354L, 8L)).thenReturn(List.of());
        when(registrationAuditMapper.selectVoByRegistrationId(354L)).thenReturn(List.of());

        BasketRegistrationAuditDetailVo detail = registrationAuditService.selectDetail(354L);

        assertEquals(RegistrationStatusEnum.APPROVED.getCode(), detail.getRegistrationStatus());
        assertEquals(2, detail.getCurrentEligibleCount());
        assertNotNull(detail.getPlayers());
        assertNotNull(detail.getPendingMembers());
        assertEquals(0, detail.getSubmittedRosterCount());
    }

    @Test
    void rejectThenRepeatRejectDoesNotRewriteRoster() {
        BasketRegistration pending = pendingRegistration();
        BasketRegistration alreadyRejected = pendingRegistration();
        alreadyRejected.setRegistrationStatus(RegistrationStatusEnum.REJECTED.getCode());
        when(registrationMapper.selectById(354L)).thenReturn(pending, alreadyRejected);
        when(registrationMapper.updateById(any())).thenReturn(1);
        BasketRegistrationAuditRejectReq req = new BasketRegistrationAuditRejectReq();
        req.setRejectReason("人数不足");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(1L);
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            registrationAuditService.reject(354L, req);
            ServiceException repeat = assertThrows(ServiceException.class,
                    () -> registrationAuditService.reject(354L, req));
            assertEquals("仅待审核状态可驳回", repeat.getMessage());
        }

        verify(registrationMapper).updateById(any());
        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
        verify(registrationPlayerMapper, never()).softDelete(any());
    }

    @Test
    void approveAfterRejectIsRejected() {
        BasketRegistration rejected = pendingRegistration();
        rejected.setRegistrationStatus(RegistrationStatusEnum.REJECTED.getCode());
        when(registrationMapper.selectById(354L)).thenReturn(rejected);

        ServiceException ex = assertThrows(ServiceException.class, () -> registrationAuditService.approve(354L));

        assertEquals("仅待审核状态可通过", ex.getMessage());
        verify(registrationMapper, never()).updateById(any());
    }

    @Test
    void lockedIncrementalUpdateKeepsExistingEventPlayerId() {
        BasketRegistration registration = approvedRegistration();
        when(eligibilityQueries.frozen(7L)).thenReturn(false);
        BasketEventTeam eventTeam = eventTeam();
        when(eventTeamMapper.selectByDivisionSourceTeam(7L, 8L)).thenReturn(eventTeam);
        when(registrationPlayerMapper.selectByRegistrationId(4L)).thenReturn(List.of(rosterPlayer(11L)));
        when(registrationPlayerMapper.countActiveMember(8L, 11L)).thenReturn(1);
        BasketEventPlayer existing = eventPlayer(8801L, 11L, 0);
        when(eventPlayerMapper.selectAnyByEventTeamAndSource(50L, 11L)).thenReturn(existing);
        when(eventPlayerMapper.selectByEventTeamId(50L)).thenReturn(List.of(existing));

        eventPlayerSync.syncWritable(registration);

        ArgumentCaptor<BasketEventPlayer> updated = ArgumentCaptor.forClass(BasketEventPlayer.class);
        verify(eventPlayerMapper).updateById(updated.capture());
        assertEquals(8801L, updated.getValue().getId());
        verify(eventPlayerMapper, never()).insert(any());
        verify(eventPlayerMapper, never()).softDelete(any());
    }

    @Test
    void reviveKeepsOriginalEventPlayerId() {
        BasketRegistration registration = approvedRegistration();
        when(eligibilityQueries.frozen(7L)).thenReturn(false);
        when(eventTeamMapper.selectByDivisionSourceTeam(7L, 8L)).thenReturn(eventTeam());
        when(registrationPlayerMapper.selectByRegistrationId(4L)).thenReturn(List.of(rosterPlayer(11L)));
        when(registrationPlayerMapper.countActiveMember(8L, 11L)).thenReturn(1);
        BasketEventPlayer deleted = eventPlayer(8801L, 11L, 1);
        when(eventPlayerMapper.selectAnyByEventTeamAndSource(50L, 11L)).thenReturn(deleted);
        when(eventPlayerMapper.selectByEventTeamId(50L)).thenReturn(List.of());

        eventPlayerSync.syncWritable(registration);

        ArgumentCaptor<BasketEventPlayer> revived = ArgumentCaptor.forClass(BasketEventPlayer.class);
        verify(eventPlayerMapper).revive(revived.capture());
        assertEquals(8801L, revived.getValue().getId());
        verify(eventPlayerMapper, never()).insert(any());
    }

    @Test
    void startedOrArchivedOrStatsDoNotRewriteEventPlayers() {
        assertTrue(RosterChangeWindowSupport.isRosterFrozen(DivisionStatusEnum.FINISHED.getCode(), 0, 0));
        assertTrue(RosterChangeWindowSupport.isRosterFrozen(DivisionStatusEnum.ARCHIVED.getCode(), 0, 0));
        assertTrue(RosterChangeWindowSupport.isRosterFrozen(DivisionStatusEnum.PLAYING.getCode(), 1, 0));
        assertTrue(RosterChangeWindowSupport.isRosterFrozen(DivisionStatusEnum.REGISTRATION_OPEN.getCode(), 0, 1));
        assertFalse(RosterChangeWindowSupport.isRosterFrozen(DivisionStatusEnum.REGISTRATION_OPEN.getCode(), 0, 0));

        BasketRegistration registration = approvedRegistration();
        when(eligibilityQueries.frozen(7L)).thenReturn(true);
        eventPlayerSync.syncWritable(registration);
        verify(eventTeamMapper, never()).selectByDivisionSourceTeam(any(), any());
        verify(eventPlayerMapper, never()).insert(any());
        verify(eventPlayerMapper, never()).updateById(any());
        verify(eventPlayerMapper, never()).softDelete(any());
        verify(eventPlayerMapper, never()).revive(any());
    }

    @Test
    void referencedEventPlayerBlocksHistoryRewrite() {
        BasketRegistration registration = approvedRegistration();
        when(eligibilityQueries.frozen(7L)).thenReturn(false);
        when(eventTeamMapper.selectByDivisionSourceTeam(7L, 8L)).thenReturn(eventTeam());
        when(registrationPlayerMapper.selectByRegistrationId(4L)).thenReturn(List.of());
        BasketEventPlayer referenced = eventPlayer(8802L, 99L, 0);
        when(eventPlayerMapper.selectByEventTeamId(50L)).thenReturn(List.of(referenced));
        when(matchPlayerStatMapper.countByEventPlayerId(8802L)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class, () -> eventPlayerSync.syncWritable(registration));

        assertEquals("赛事球员已被比赛数据引用，不能改写正式名单", ex.getMessage());
        verify(eventPlayerMapper, never()).softDelete(any());
    }

    @Test
    void startedMatchSqlCoversPlayingFinishedAndForfeited() throws IOException {
        String xml;
        try (var input = getClass().getResourceAsStream("/mapper/basket/v4/BasketV4MatchMapper.xml")) {
            assertNotNull(input);
            xml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        int start = xml.indexOf("countStartedByDivisionId");
        int end = xml.indexOf("</select>", start);
        String query = xml.substring(start, end);
        assertTrue(query.contains("'PLAYING'"));
        assertTrue(query.contains("'FINISHED'"));
        assertTrue(query.contains("'FORFEITED'"));
    }

    @Test
    void syncEndpointRequiresDivisionEdit() throws NoSuchMethodException {
        PreAuthorize auth = BasketV4TeamRosterController.class
                .getMethod("sync", Long.class)
                .getAnnotation(PreAuthorize.class);
        assertNotNull(auth);
        assertTrue(auth.value().contains(BasketEventV4Perms.DIVISION_EDIT));
        assertTrue(auth.value().contains("basket:v4Division:edit"));
        EnableMethodSecurity methodSecurity = SecurityConfig.class.getAnnotation(EnableMethodSecurity.class);
        assertNotNull(methodSecurity);
        assertTrue(methodSecurity.prePostEnabled());
    }

    @Test
    void autoSyncExpiresOldPendingChangeInsteadOfApplyingIt() {
        BasketRegistration registration = approvedRegistration();
        when(registrationMapper.selectById(4L)).thenReturn(registration);
        when(eligibilityQueries.frozen(7L)).thenReturn(false);
        when(rosterVersionService.getCurrentLockedVersion(7L)).thenReturn(null);
        when(divisionMapper.selectById(7L)).thenReturn(openDivision());
        when(registrationPlayerMapper.selectByRegistrationId(4L)).thenReturn(List.of());
        when(teamMemberMapper.selectCurrentMembersForAudit(8L)).thenReturn(List.of());
        when(rosterChangeMapper.selectPendingIdsByRegistrationId(4L)).thenReturn(List.of(77L));

        RosterSyncResultVo result = openRosterSync.syncRegistration(4L);

        assertFalse(result.isSkipped());
        verify(rosterChangeMapper).markRejected(eq(77L), isNull(),
                eq("最终名单锁定前已按当前球队成员自动同步，原名单变更申请失效"), any());
        verify(rosterChangeMapper, never()).selectPlayers(any());
        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
        verify(eventPlayerSyncMock, never()).syncWritable(any());
    }

    @Test
    void rejectedPendingChangeCannotOverwriteRoster() {
        BasketRegistrationRosterChange change = pendingChange();
        change.setChangeStatus("REJECTED");
        when(rosterChangeMapper.selectById(5L)).thenReturn(change);

        ServiceException ex = assertThrows(ServiceException.class, () -> rosterChangeAuditService.approve(5L));

        assertEquals("名单变更已处理，请刷新", ex.getMessage());
        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
        verify(eventPlayerSyncMock, never()).syncWritable(any());
    }

    @Test
    void lockedRosterChangeDelegatesToIdPreservingSync() {
        BasketRegistrationRosterChange change = pendingChange();
        BasketRegistration registration = approvedRegistration();
        registration.setLineupVersion(0);
        when(rosterChangeMapper.selectById(5L)).thenReturn(change);
        when(divisionMapper.selectByIdForUpdate(7L)).thenReturn(openDivision());
        when(divisionMapper.selectById(7L)).thenReturn(openDivision());
        when(registrationMapper.selectById(4L)).thenReturn(registration);
        when(rosterVersionService.getCurrentLockedVersion(7L)).thenReturn(new BasketDivisionRosterVersion());
        when(matchMapper.countStartedByDivisionId(7L)).thenReturn(0);
        BasketRegistrationRosterChangePlayer target = new BasketRegistrationRosterChangePlayer();
        target.setPlayerId(11L);
        target.setIsCaptain(1);
        when(rosterChangeMapper.selectPlayers(5L)).thenReturn(List.of(target));
        when(registrationPlayerMapper.countActivePlayerInDivision(7L, 11L, 4L)).thenReturn(0);
        BasketPlayerProfile profile = new BasketPlayerProfile();
        profile.setMemberId(12L);
        profile.setGameNickname("CNPA");
        when(participationSupport.requireCertifiedPlayer(11L)).thenReturn(profile);
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setId(13L);
        account.setPlatform("PS");
        account.setDisplayGameId("Gzy-DL");
        when(participationSupport.requirePlatformIdentity(11L, "PS_XBOX")).thenReturn(account);
        when(registrationMapper.bumpLineupVersion(4L, 1, 0)).thenReturn(1);
        when(rosterChangeMapper.markApproved(eq(5L), any(), any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(1L);
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            rosterChangeAuditService.approve(5L);
        }

        verify(eventPlayerSyncMock).syncWritable(registration);
    }

    @Test
    void frozenDivisionSkipsAutomaticRosterWrite() {
        when(registrationMapper.selectById(4L)).thenReturn(approvedRegistration());
        when(eligibilityQueries.frozen(7L)).thenReturn(true);

        RosterSyncResultVo result = openRosterSync.syncRegistration(4L);

        assertTrue(result.isSkipped());
        assertEquals("赛事已开赛或已结束，不能自动改名单", result.getSkipReason());
        verify(registrationPlayerMapper, never()).softDelete(any());
        verify(rosterChangeMapper, never()).markRejected(any(), any(), any(), any());
    }

    private static BasketRegistration approvedRegistration() {
        BasketRegistration registration = pendingRegistration();
        registration.setId(4L);
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        return registration;
    }

    private static BasketRegistration pendingRegistration() {
        BasketRegistration registration = new BasketRegistration();
        registration.setId(354L);
        registration.setDivisionId(7L);
        registration.setSeasonId(3L);
        registration.setTeamId(8L);
        registration.setRegistrationStatus(RegistrationStatusEnum.PENDING_REVIEW.getCode());
        registration.setLineupVersion(0);
        registration.setIsDelete(0);
        return registration;
    }

    private static BasketDivision openDivision() {
        BasketDivision division = new BasketDivision();
        division.setId(7L);
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setPlatformCode("PS_XBOX");
        division.setLineupMinCount(5);
        division.setLineupMaxCount(8);
        return division;
    }

    private static BasketRegistrationRosterChange pendingChange() {
        BasketRegistrationRosterChange change = new BasketRegistrationRosterChange();
        change.setId(5L);
        change.setRegistrationId(4L);
        change.setDivisionId(7L);
        change.setSeasonId(3L);
        change.setChangeStatus("PENDING");
        change.setBaseLineupVersion(0);
        return change;
    }

    private static BasketEventTeam eventTeam() {
        BasketEventTeam eventTeam = new BasketEventTeam();
        eventTeam.setId(50L);
        eventTeam.setSeasonId(3L);
        eventTeam.setDivisionId(7L);
        eventTeam.setRosterVersionId(9L);
        return eventTeam;
    }

    private static BasketRegistrationPlayer rosterPlayer(Long playerId) {
        BasketRegistrationPlayer player = new BasketRegistrationPlayer();
        player.setPlayerId(playerId);
        player.setMemberId(12L);
        player.setPlatformIdentityId(13L);
        player.setPlatformCode("PS");
        player.setGameIdSnapshot("Gzy-DL");
        player.setPlayerNameSnapshot("CNPA");
        player.setIsCaptain(1);
        player.setIsStarter(1);
        player.setIsDelete(0);
        return player;
    }

    private static BasketEventPlayer eventPlayer(Long id, Long sourcePlayerId, int deleted) {
        BasketEventPlayer eventPlayer = new BasketEventPlayer();
        eventPlayer.setId(id);
        eventPlayer.setSourcePlayerId(sourcePlayerId);
        eventPlayer.setIsDelete(deleted);
        return eventPlayer;
    }
}
