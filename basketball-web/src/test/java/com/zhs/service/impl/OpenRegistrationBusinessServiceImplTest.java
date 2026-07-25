package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.dao.BasketTournamentOpenRegistrationDao;
import com.zhs.exception.ServiceException;
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
class OpenRegistrationBusinessServiceImplTest {

    @Mock
    private BasketTournamentOpenRegistrationDao openRegistrationDao;
    @InjectMocks
    private OpenRegistrationBusinessServiceImpl service;

    @Test
    void assertNoDuplicateOpenRegistration_passesWhenNoRecord() {
        when(openRegistrationDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertDoesNotThrow(() -> service.assertNoDuplicateOpenRegistration(1L, 100L));
    }

    @Test
    void assertNoDuplicateOpenRegistration_rejectsExistingRecord() {
        when(openRegistrationDao.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        assertThrows(ServiceException.class,
                () -> service.assertNoDuplicateOpenRegistration(1L, 100L));
    }
}
