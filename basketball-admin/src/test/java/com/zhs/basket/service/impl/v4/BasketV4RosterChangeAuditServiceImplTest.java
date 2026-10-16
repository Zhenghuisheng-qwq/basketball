package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketRegistration;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChange;
import com.zhs.basket.domain.v4.BasketRegistrationRosterChangePlayer;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.RegistrationStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationAuditMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionPlayerStatMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationRosterChangeMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.mapper.v4.BasketV4MatchPlayerStatMapper;
import com.zhs.basket.service.v4.IBasketV4RosterVersionService;
import com.zhs.basket.support.v4.BasketV4ParticipationSupport;
import com.zhs.basket.support.v4.PreMatchEventPlayerSync;
import com.zhs.basket.dto.v4.BasketRegistrationAuditRejectReq;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketV4RosterChangeAuditServiceImplTest {

    @InjectMocks
    private BasketV4RosterChangeAuditServiceImpl service;

    @Mock
    private BasketRegistrationRosterChangeMapper rosterChangeMapper;
    @Mock
    private BasketRegistrationMapper registrationMapper;
    @Mock
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Mock
    private BasketRegistrationAuditMapper registrationAuditMapper;
    @Mock
    private BasketDivisionMapper divisionMapper;
    @Mock
    private BasketV4MatchMapper matchMapper;
    @Mock
    private BasketV4MatchPlayerStatMapper matchPlayerStatMapper;
    @Mock
    private BasketMatchSubmissionPlayerStatMapper submissionPlayerStatMapper;
    @Mock
    private BasketV4ParticipationSupport participationSupport;
    @Mock
    private IBasketV4RosterVersionService rosterVersionService;
    @Mock
    private PreMatchEventPlayerSync eventPlayerSync;

    @Test
    void versionConflictDoesNotReplaceOfficialRoster() {
        when(rosterChangeMapper.selectById(5L)).thenReturn(pendingChange(1));
        when(divisionMapper.selectByIdForUpdate(7L)).thenReturn(openDivision());
        BasketRegistration registration = approvedRegistration(2);
        when(registrationMapper.selectById(4L)).thenReturn(registration);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(5L));

        assertEquals("原名单已变化，请让队长重新提交名单变更", ex.getMessage());
        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
    }

    @Test
    void occupiedPlayerBlocksApproval() {
        when(rosterChangeMapper.selectById(5L)).thenReturn(pendingChange(0));
        when(divisionMapper.selectByIdForUpdate(7L)).thenReturn(openDivision());
        when(divisionMapper.selectById(7L)).thenReturn(openDivision());
        when(registrationMapper.selectById(4L)).thenReturn(approvedRegistration(0));
        when(rosterVersionService.getCurrentLockedVersion(7L)).thenReturn(new BasketDivisionRosterVersion());
        when(matchMapper.countStartedByDivisionId(7L)).thenReturn(0);
        when(matchPlayerStatMapper.countByDivisionId(7L)).thenReturn(0);
        BasketRegistrationRosterChangePlayer player = new BasketRegistrationRosterChangePlayer();
        player.setPlayerId(99L);
        player.setIsCaptain(1);
        when(rosterChangeMapper.selectPlayers(5L)).thenReturn(List.of(player));
        when(registrationPlayerMapper.countActivePlayerInDivision(7L, 99L, 4L)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.approve(5L));

        assertEquals("球员已在该 Division 参与其他报名", ex.getMessage());
        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
    }

    @Test
    void rejectKeepsOfficialRoster() {
        when(rosterChangeMapper.selectById(5L)).thenReturn(pendingChange(0));
        when(rosterChangeMapper.markRejected(org.mockito.ArgumentMatchers.eq(5L), any(), org.mockito.ArgumentMatchers.eq("人数不足"), any()))
                .thenReturn(1);
        when(registrationMapper.selectById(4L)).thenReturn(approvedRegistration(0));
        BasketRegistrationAuditRejectReq req = new BasketRegistrationAuditRejectReq();
        req.setRejectReason("人数不足");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::getUserId).thenReturn(1L);
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            service.reject(5L, req);
        }

        verify(registrationPlayerMapper, never()).deleteByRegistrationId(any());
        verify(registrationMapper, never()).bumpLineupVersion(any(), any(), any());
    }

    private BasketRegistrationRosterChange pendingChange(int version) {
        BasketRegistrationRosterChange change = new BasketRegistrationRosterChange();
        change.setId(5L);
        change.setRegistrationId(4L);
        change.setDivisionId(7L);
        change.setSeasonId(3L);
        change.setTeamId(8L);
        change.setChangeStatus("PENDING");
        change.setBaseLineupVersion(version);
        return change;
    }

    private BasketRegistration approvedRegistration(int version) {
        BasketRegistration registration = new BasketRegistration();
        registration.setId(4L);
        registration.setDivisionId(7L);
        registration.setSeasonId(3L);
        registration.setTeamId(8L);
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        registration.setLineupVersion(version);
        registration.setCaptainMemberId(9L);
        return registration;
    }

    private BasketDivision openDivision() {
        BasketDivision division = new BasketDivision();
        division.setId(7L);
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setPlatformCode("PS");
        division.setRegistrationStartTime(new Date(System.currentTimeMillis() - 86_400_000L));
        division.setRegistrationEndTime(new Date(System.currentTimeMillis() + 86_400_000L));
        division.setLineupMinCount(1);
        division.setLineupMaxCount(8);
        return division;
    }
}
