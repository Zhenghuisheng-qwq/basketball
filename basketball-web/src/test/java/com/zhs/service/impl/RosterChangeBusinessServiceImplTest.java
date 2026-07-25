package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTournamentRosterChangeRequestDao;
import com.zhs.dao.BasketTournamentSeasonRosterDao;
import com.zhs.enums.RosterLockStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeamSeasonProfile;
import com.zhs.model.BasketTournamentSeason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RosterChangeBusinessServiceImplTest {

    @Mock
    private BasketTournamentRosterChangeRequestDao rosterChangeRequestDao;
    @Mock
    private BasketTournamentSeasonRosterDao seasonRosterDao;
    @InjectMocks
    private RosterChangeBusinessServiceImpl service;

    @Test
    void validateEventTeamNotLocked_passesWhenUnlocked() {
        BasketTeamSeasonProfile eventTeam = new BasketTeamSeasonProfile();
        eventTeam.setRosterLockStatus(RosterLockStatusEnum.UNLOCKED.getCode());
        assertDoesNotThrow(() -> service.validateEventTeamNotLocked(eventTeam));
    }

    @Test
    void validateEventTeamNotLocked_rejectsLockedTeam() {
        BasketTeamSeasonProfile eventTeam = new BasketTeamSeasonProfile();
        eventTeam.setRosterLockStatus(RosterLockStatusEnum.LOCKED.getCode());
        assertThrows(ServiceException.class, () -> service.validateEventTeamNotLocked(eventTeam));
    }

    @Test
    void validateTransferWindow_rejectsWhenDisabled() {
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setTransferEnabled(0);
        assertThrows(ServiceException.class,
                () -> service.validateTransferWindow(season, new Date()));
    }

    @Test
    void validateTransferWindow_rejectsWhenBeforeWindow() {
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setTransferEnabled(1);
        season.setTransferStartTime(new GregorianCalendar(2099, Calendar.JANUARY, 1).getTime());
        assertThrows(ServiceException.class,
                () -> service.validateTransferWindow(season, new Date()));
    }

    @Test
    void validateFreeAgentQuota_rejectsWhenExceeded() {
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setId(1L);
        season.setMaxFreeAgentAddPerTeam(2);
        when(rosterChangeRequestDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        assertThrows(ServiceException.class,
                () -> service.validateFreeAgentQuota(season, 10L));
    }

    @Test
    void validateRosterCapacity_rejectsWhenFull() {
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setMaxRosterCount(8);
        when(seasonRosterDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(8L);
        assertThrows(ServiceException.class,
                () -> service.validateRosterCapacity(season, 10L));
    }
}
