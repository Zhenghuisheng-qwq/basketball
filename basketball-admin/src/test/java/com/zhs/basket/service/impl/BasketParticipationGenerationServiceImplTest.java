package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.RegistrationStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.support.EventTeamNoGenerator;
import com.zhs.basket.support.RosterNoGenerator;
import com.zhs.basket.vo.RegistrationPlayerVo;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketParticipationGenerationServiceImplTest {

    @Mock private BasketTournamentRegistrationMapper registrationMapper;
    @Mock private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Mock private BasketTournamentRegistrationAuditMapper registrationAuditMapper;
    @Mock private BasketTeamMapper teamMapper;
    @Mock private BasketTournamentMapper tournamentMapper;
    @Mock private BasketTournamentSeasonMapper seasonMapper;
    @Mock private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;
    @Mock private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Mock private BasketPlayerProfileMapper profileMapper;
    @Mock private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Mock private BasketCaptainQualificationMapper captainQualificationMapper;
    @Mock private EventTeamNoGenerator eventTeamNoGenerator;
    @Mock private RosterNoGenerator rosterNoGenerator;

    @InjectMocks
    private BasketParticipationGenerationServiceImpl service;

    private MockedStatic<SecurityUtils> securityUtils;
    private BasketTournamentRegistration registration;
    private BasketTeam team;
    private BasketTeamSeasonProfile existingProfile;
    private BasketTournamentSeason season;
    private BasketTournament tournament;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

        registration = new BasketTournamentRegistration();
        registration.setId(1L);
        registration.setSeasonId(10L);
        registration.setTeamId(20L);
        registration.setCaptainPlayerId(30L);
        registration.setCaptainMemberId(40L);
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());
        registration.setAuditTime(new Date());

        team = new BasketTeam();
        team.setId(20L);
        team.setTeamName("测试球队");
        team.setGameTerminal("PC");

        season = new BasketTournamentSeason();
        season.setId(10L);
        season.setTournamentId(100L);

        tournament = new BasketTournament();
        tournament.setId(100L);
        tournament.setGameTerminal("PC");

        existingProfile = new BasketTeamSeasonProfile();
        existingProfile.setId(100L);
        existingProfile.setRegistrationId(1L);
    }

    @AfterEach
    void tearDown() {
        if (securityUtils != null) {
            securityUtils.close();
        }
    }

    private void stubCommonContext() {
        when(teamMapper.selectById(20L)).thenReturn(team);
        when(seasonMapper.selectById(10L)).thenReturn(season);
        when(tournamentMapper.selectById(100L)).thenReturn(tournament);
        when(captainQualificationMapper.selectActiveGrantForEvent(40L, 20L, 10L)).thenReturn(new BasketCaptainQualification());
        when(registrationPlayerMapper.selectDetailByRegistrationId(1L)).thenReturn(Collections.emptyList());
    }

    @Test
    void generateFromTeamRegistration_idempotentWhenProfileExists() {
        when(registrationMapper.selectById(1L)).thenReturn(registration);
        stubCommonContext();
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(existingProfile);
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(Collections.emptyList());
        when(seasonRosterMapper.selectByRegistrationId(1L)).thenReturn(Collections.emptyList());

        Long eventTeamId = service.generateFromTeamRegistration(1L, 999L, "remark");

        assertEquals(100L, eventTeamId);
        verify(teamSeasonProfileMapper).updateRosterLockStatus(eq(100L), eq(1), any(Date.class));
        verify(seasonRosterMapper).updateLockStatusByEventTeamId(eq(100L), eq(1), any(Date.class));
        verify(teamSeasonProfileMapper, never()).insert(any());
    }

    @Test
    void generateFromTeamRegistration_createsProfileAndRosterOnFirstRun() {
        BasketTournamentRegistrationPlayer player = new BasketTournamentRegistrationPlayer();
        player.setId(501L);
        player.setPlayerId(30L);
        player.setMemberId(40L);
        player.setRosterRole("PLAYER");
        player.setIsCaptain(1);
        player.setIsStarting(1);

        RegistrationPlayerVo detail = new RegistrationPlayerVo();
        detail.setPlayerId(30L);
        detail.setPlayerNickname("队长A");
        detail.setPositionCode("PG");

        when(registrationMapper.selectById(1L)).thenReturn(registration);
        stubCommonContext();
        when(registrationPlayerMapper.selectDetailByRegistrationId(1L)).thenReturn(List.of(detail));
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(null);
        when(eventTeamNoGenerator.generateEventTeamNo(10L)).thenReturn("CNPA-ET-000001");
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(List.of(player));
        when(seasonRosterMapper.countActiveBySeasonAndPlayer(10L, 30L)).thenReturn(0);
        when(profileMapper.selectById(30L)).thenReturn(new BasketPlayerProfile());
        when(profileMapper.selectAvatarFileIdByMemberId(40L)).thenReturn(888L);
        when(platformAccountMapper.selectByPlayerId(30L)).thenReturn(Collections.emptyList());
        when(rosterNoGenerator.generateRosterNo()).thenReturn("CNPA-ER-000001");
        doAnswer(invocation -> {
            BasketTeamSeasonProfile p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(teamSeasonProfileMapper).insert(any(BasketTeamSeasonProfile.class));

        Long eventTeamId = service.generateFromTeamRegistration(1L, 999L, "remark");

        assertEquals(100L, eventTeamId);
        verify(teamSeasonProfileMapper).insert(argThat(profile ->
                Integer.valueOf(1).equals(profile.getRosterLockStatus())
                        && "队长A".equals(profile.getCaptainNameSnapshot())));
        verify(seasonRosterMapper).insert(argThat(roster ->
                "10:30".equals(roster.getActiveSlot())
                        && Long.valueOf(888L).equals(roster.getAvatarFileIdSnapshot())
                        && "PG".equals(roster.getPositionSnapshot())));
        verify(registrationAuditMapper).insert(argThat(audit -> "GENERATE_EVENT_TEAM".equals(audit.getAuditAction())));
        verify(registrationAuditMapper).insert(argThat(audit -> "GENERATE_EVENT_PLAYER".equals(audit.getAuditAction())));
    }

    @Test
    void generateFromTeamRegistration_createsMissingRosterOnlyWhenProfileExists() {
        BasketTournamentRegistrationPlayer player = new BasketTournamentRegistrationPlayer();
        player.setId(502L);
        player.setPlayerId(31L);
        player.setMemberId(41L);

        when(registrationMapper.selectById(1L)).thenReturn(registration);
        stubCommonContext();
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(existingProfile);
        when(seasonRosterMapper.selectByRegistrationId(1L)).thenReturn(Collections.emptyList());
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(List.of(player));
        when(seasonRosterMapper.countActiveBySeasonAndPlayer(10L, 31L)).thenReturn(0);
        when(profileMapper.selectById(31L)).thenReturn(new BasketPlayerProfile());
        when(platformAccountMapper.selectByPlayerId(31L)).thenReturn(Collections.emptyList());
        when(rosterNoGenerator.generateRosterNo()).thenReturn("CNPA-ER-000002");

        service.generateFromTeamRegistration(1L, 999L, "remark");

        verify(teamSeasonProfileMapper, never()).insert(any());
        verify(seasonRosterMapper).insert(argThat(roster -> "10:31".equals(roster.getActiveSlot())));
    }
}

