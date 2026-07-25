package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.RegistrationStatusEnum;
import com.zhs.basket.mapper.*;
import com.zhs.basket.support.EventTeamNoGenerator;
import com.zhs.basket.support.RosterNoGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketParticipationGenerationServiceImplTest {

    @Mock private BasketTournamentRegistrationMapper registrationMapper;
    @Mock private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Mock private BasketTeamMapper teamMapper;
    @Mock private BasketTeamSeasonProfileMapper teamSeasonProfileMapper;
    @Mock private BasketTournamentSeasonRosterMapper seasonRosterMapper;
    @Mock private BasketPlayerProfileMapper profileMapper;
    @Mock private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @Mock private EventTeamNoGenerator eventTeamNoGenerator;
    @Mock private RosterNoGenerator rosterNoGenerator;

    @InjectMocks
    private BasketParticipationGenerationServiceImpl service;

    private BasketTournamentRegistration registration;
    private BasketTeam team;
    private BasketTeamSeasonProfile existingProfile;

    @BeforeEach
    void setUp() {
        registration = new BasketTournamentRegistration();
        registration.setId(1L);
        registration.setSeasonId(10L);
        registration.setTeamId(20L);
        registration.setCaptainPlayerId(30L);
        registration.setRegistrationStatus(RegistrationStatusEnum.APPROVED.getCode());

        team = new BasketTeam();
        team.setId(20L);
        team.setTeamName("测试球队");
        team.setGameTerminal("PC");

        existingProfile = new BasketTeamSeasonProfile();
        existingProfile.setId(100L);
        existingProfile.setRegistrationId(1L);
    }

    @Test
    void generateFromTeamRegistration_idempotentWhenProfileExists() {
        when(registrationMapper.selectById(1L)).thenReturn(registration);
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(existingProfile);
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(Collections.emptyList());
        when(seasonRosterMapper.selectByRegistrationId(1L)).thenReturn(Collections.emptyList());

        Long eventTeamId = service.generateFromTeamRegistration(1L, 999L, "remark");

        assertEquals(100L, eventTeamId);
        verify(teamSeasonProfileMapper, never()).insert(any());
        verify(eventTeamNoGenerator, never()).generateEventTeamNo(anyLong());
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

        when(registrationMapper.selectById(1L)).thenReturn(registration);
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(null);
        when(teamMapper.selectById(20L)).thenReturn(team);
        when(eventTeamNoGenerator.generateEventTeamNo(10L)).thenReturn("CNPA-ET-000001");
        when(registrationPlayerMapper.selectByRegistrationId(1L)).thenReturn(List.of(player));
        when(seasonRosterMapper.countActiveBySeasonAndPlayer(10L, 30L)).thenReturn(0);
        when(profileMapper.selectById(30L)).thenReturn(new BasketPlayerProfile());
        when(platformAccountMapper.selectByPlayerId(30L)).thenReturn(Collections.emptyList());
        when(rosterNoGenerator.generateRosterNo()).thenReturn("CNPA-ER-000001");
        doAnswer(invocation -> {
            BasketTeamSeasonProfile p = invocation.getArgument(0);
            p.setId(100L);
            return 1;
        }).when(teamSeasonProfileMapper).insert(any(BasketTeamSeasonProfile.class));

        Long eventTeamId = service.generateFromTeamRegistration(1L, 999L, "remark");

        assertEquals(100L, eventTeamId);
        verify(teamSeasonProfileMapper).insert(any(BasketTeamSeasonProfile.class));
        verify(seasonRosterMapper).insert(argThat(roster ->
                "10:30".equals(roster.getActiveSlot())
                        && Integer.valueOf(1).equals(roster.getRosterStatus())));
    }

    @Test
    void generateFromTeamRegistration_createsMissingRosterOnlyWhenProfileExists() {
        BasketTournamentRegistrationPlayer player = new BasketTournamentRegistrationPlayer();
        player.setId(502L);
        player.setPlayerId(31L);
        player.setMemberId(41L);

        when(registrationMapper.selectById(1L)).thenReturn(registration);
        when(teamSeasonProfileMapper.selectByRegistrationId(1L)).thenReturn(existingProfile);
        when(teamMapper.selectById(20L)).thenReturn(team);
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
