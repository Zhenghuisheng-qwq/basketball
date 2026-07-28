package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerPlatformChangeRequestDao;
import com.zhs.dao.BasketPlayerPlatformIdentityHistoryDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.PlatformIdentityHistoryChangeTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerPlatformChangeRequest;
import com.zhs.model.BasketPlayerPlatformIdentityHistory;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.response.player.PlatformChangeHistoryItemResp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerPlatformAccountServiceImplTest {

    @Mock
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Mock
    private BasketPlayerPlatformChangeRequestDao platformChangeRequestDao;
    @Mock
    private BasketPlayerPlatformIdentityHistoryDao platformIdentityHistoryDao;
    @Mock
    private BasketPlayerProfileDao playerProfileDao;
    @InjectMocks
    private PlayerPlatformAccountServiceImpl service;

    @Test
    void assertGameIdAvailable_passesWhenNoConflict() {
        when(platformAccountDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(platformChangeRequestDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertDoesNotThrow(() -> service.assertGameIdAvailable("PC", "player123", 99L));
    }

    @Test
    void assertGameIdAvailable_rejectsActiveAccount() {
        when(platformAccountDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(ServiceException.class,
                () -> service.assertGameIdAvailable("PC", "player123", 99L));
    }

    @Test
    void assertGameIdAvailable_rejectsPendingChangeRequest() {
        when(platformAccountDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(platformChangeRequestDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThrows(ServiceException.class,
                () -> service.assertGameIdAvailable("PC", "player123", 99L));
    }

    @Test
    void listChangeHistory_returnsInitialBindByPlayerId() {
        Long memberId = 100L;
        Long playerId = 200L;
        Date effectiveTime = new Date();

        BasketPlayerProfile profile = new BasketPlayerProfile();
        profile.setId(playerId);
        profile.setMemberId(memberId);
        profile.setCertificationStatus(3);

        BasketPlayerPlatformIdentityHistory history = new BasketPlayerPlatformIdentityHistory();
        history.setId(1L);
        history.setPlayerId(playerId);
        history.setPlatformCode("PS");
        history.setChangeType(PlatformIdentityHistoryChangeTypeEnum.INITIAL_BIND.getCode());
        history.setNewGameId("dnakldbnas");
        history.setEffectiveTime(effectiveTime);

        when(playerProfileDao.selectOne(any(LambdaQueryWrapper.class))).thenReturn(profile);
        when(platformIdentityHistoryDao.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(history));

        List<PlatformChangeHistoryItemResp> result = service.listChangeHistory(memberId);

        assertEquals(1, result.size());
        assertEquals("INITIAL_BIND", result.get(0).getChangeType());
        assertEquals("首次绑定", result.get(0).getChangeTypeName());
        assertEquals("PS", result.get(0).getPlatformCode());
        assertEquals("dnakldbnas", result.get(0).getNewGameId());
        assertEquals(effectiveTime, result.get(0).getEffectiveTime());
    }
}
