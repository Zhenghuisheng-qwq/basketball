package com.zhs.service.impl.v4;

import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketEventPlayerDao;
import com.zhs.dao.v4.BasketMatchSubmissionPlayerStatDao;
import com.zhs.dao.v4.BasketRegistrationDao;
import com.zhs.dao.v4.BasketRegistrationPlayerDao;
import com.zhs.dao.v4.BasketV4MatchDao;
import com.zhs.dao.v4.BasketV4MatchPlayerStatDao;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketRegistration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventRosterHistoryGuardTest {

    @InjectMocks
    private EventRosterMembershipSync sync;

    @Mock
    private BasketRegistrationDao registrationDao;
    @Mock
    private BasketRegistrationPlayerDao registrationPlayerDao;
    @Mock
    private BasketV4MatchDao matchDao;
    @Mock
    private BasketV4MatchPlayerStatDao playerStatDao;
    @Mock
    private BasketMatchSubmissionPlayerStatDao submissionPlayerStatDao;
    @Mock
    private BasketDivisionDao divisionDao;
    @Mock
    private BasketEventPlayerDao eventPlayerDao;

    @Test
    void startedStatusesCoverPlayingFinishedAndForfeited() throws Exception {
        Field field = EventRosterMembershipSync.class.getDeclaredField("STARTED");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> started = (List<String>) field.get(null);
        assertEquals(List.of("PLAYING", "FINISHED", "FORFEITED"), started);
    }

    @Test
    void finishedDivisionDoesNotChangeRegistrationOrEventPlayers() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("FINISHED"));

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    @Test
    void archivedDivisionDoesNotChangeRegistrationOrEventPlayers() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("ARCHIVED"));

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    @Test
    void startedMatchDoesNotChangeRegistrationOrEventPlayers() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("PLAYING"));
        when(matchDao.selectCount(any())).thenReturn(1L);

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    @Test
    void officialStatsDoNotChangeRegistrationOrEventPlayers() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("REGISTRATION_OPEN"));
        when(matchDao.selectCount(any())).thenReturn(0L);
        when(playerStatDao.selectCount(any())).thenReturn(1L);

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    @Test
    void submissionStatsDoNotChangeRegistrationOrEventPlayers() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("REGISTRATION_OPEN"));
        when(matchDao.selectCount(any())).thenReturn(0L);
        when(playerStatDao.selectCount(any())).thenReturn(0L);
        when(submissionPlayerStatDao.selectCount(any())).thenReturn(1L);

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    @Test
    void lockedRosterDoesNotAutoSyncHistory() {
        when(registrationDao.selectList(any())).thenReturn(List.of(registration()));
        when(divisionDao.selectById(7L)).thenReturn(division("REGISTRATION_OPEN"));
        when(matchDao.selectCount(any())).thenReturn(0L);
        when(playerStatDao.selectCount(any())).thenReturn(0L);
        when(submissionPlayerStatDao.selectCount(any())).thenReturn(0L);
        when(divisionDao.countCurrentLockedRoster(7L)).thenReturn(1);

        assertEquals(0, sync.releaseFromWritableRegistrations(8L, 11L, "离队"));

        assertNoHistoryWrite();
    }

    private void assertNoHistoryWrite() {
        verify(registrationPlayerDao, never()).update(any(), any());
        verify(eventPlayerDao, never()).insert(any(com.zhs.model.v4.BasketEventPlayer.class));
        verify(eventPlayerDao, never()).updateById(any(com.zhs.model.v4.BasketEventPlayer.class));
    }

    private static BasketRegistration registration() {
        BasketRegistration registration = new BasketRegistration();
        registration.setId(4L);
        registration.setTeamId(8L);
        registration.setDivisionId(7L);
        registration.setRegistrationStatus("APPROVED");
        return registration;
    }

    private static BasketDivision division(String status) {
        BasketDivision division = new BasketDivision();
        division.setId(7L);
        division.setDivisionStatus(status);
        return division;
    }
}
