package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerPlatformChangeRequestDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerPlatformChangeRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerPlatformAccountServiceImplTest {

    @Mock
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Mock
    private BasketPlayerPlatformChangeRequestDao platformChangeRequestDao;
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
}
