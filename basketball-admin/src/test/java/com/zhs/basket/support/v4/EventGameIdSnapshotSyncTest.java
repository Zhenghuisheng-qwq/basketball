package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketPlayerPlatformAccount;
import com.zhs.basket.enums.PlatformAccountStatusEnum;
import com.zhs.basket.mapper.BasketPlayerPlatformAccountMapper;
import com.zhs.basket.mapper.v4.BasketEventPlayerMapper;
import com.zhs.basket.mapper.v4.BasketPersonalRegistrationMapper;
import com.zhs.basket.mapper.v4.BasketRegistrationPlayerMapper;
import com.zhs.basket.vo.v4.BasketEventPlayerListVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventGameIdSnapshotSyncTest {

    @Mock
    private BasketEventPlayerMapper eventPlayerMapper;
    @Mock
    private BasketRegistrationPlayerMapper registrationPlayerMapper;
    @Mock
    private BasketPersonalRegistrationMapper personalRegistrationMapper;
    @Mock
    private BasketPlayerPlatformAccountMapper platformAccountMapper;
    @InjectMocks
    private EventGameIdSnapshotSync sync;

    @Test
    void approvedChangeWritesTheNewGameIdBackToEventRosters() {
        BasketPlayerPlatformAccount account = account(21L, 7L, "PS5", "  new-ps  ");

        sync.syncApprovedChange(account);

        verify(eventPlayerMapper).syncGameIdByPlatformAccount(7L, 21L, "PS", "new-ps");
        verify(registrationPlayerMapper).syncGameIdByPlatformAccount(7L, 21L, "PS", "new-ps");
        verify(personalRegistrationMapper).syncGameAccountByPlatformAccount(7L, 21L, "PS", "new-ps");
    }

    @Test
    void blankChangeDoesNotTouchRosters() {
        sync.syncApprovedChange(null);
        sync.syncApprovedChange(account(21L, 7L, "PC", "  "));

        verify(eventPlayerMapper, never()).syncGameIdByPlatformAccount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(registrationPlayerMapper, never()).syncGameIdByPlatformAccount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(personalRegistrationMapper, never()).syncGameAccountByPlatformAccount(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void eventPlayerListShowsCurrentGameIdInsteadOfRegistrationSnapshot() {
        BasketEventPlayerListVo changed = row(7L, 21L, "PS", "old-ps");
        BasketEventPlayerListVo untouched = row(8L, 30L, "PC", "pc-old");
        when(platformAccountMapper.selectActiveByPlayerIds(anyList())).thenReturn(List.of(
                account(21L, 7L, "PS", "new-ps")));

        sync.applyEventPlayerList(List.of(changed, untouched));

        assertEquals("new-ps", changed.getGameIdSnapshot());
        assertEquals("pc-old", untouched.getGameIdSnapshot());
        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(platformAccountMapper).selectActiveByPlayerIds(ids.capture());
        assertEquals(List.of(7L, 8L), ids.getValue());
    }

    @Test
    void eventPlayerListKeepsSnapshotWhenThePlayerHasNoCurrentAccount() {
        BasketEventPlayerListVo row = row(null, null, "PC", "kept-id");

        sync.applyEventPlayerList(List.of(row));

        assertEquals("kept-id", row.getGameIdSnapshot());
        verify(platformAccountMapper, never()).selectActiveByPlayerIds(anyList());
    }

    private static BasketEventPlayerListVo row(Long playerId, Long accountId, String platform, String gameId) {
        BasketEventPlayerListVo row = new BasketEventPlayerListVo();
        row.setSourcePlayerId(playerId);
        row.setPlatformIdentityId(accountId);
        row.setPlatformCode(platform);
        row.setGameIdSnapshot(gameId);
        return row;
    }

    private static BasketPlayerPlatformAccount account(Long id, Long playerId, String platform, String gameId) {
        BasketPlayerPlatformAccount account = new BasketPlayerPlatformAccount();
        account.setId(id);
        account.setPlayerId(playerId);
        account.setPlatform(platform);
        account.setDisplayGameId(gameId);
        account.setAccountStatus(PlatformAccountStatusEnum.ACTIVE.getCode());
        return account;
    }
}
