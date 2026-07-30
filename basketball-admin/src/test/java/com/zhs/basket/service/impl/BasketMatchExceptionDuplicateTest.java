package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatchExceptionApplication;
import com.zhs.basket.mapper.BasketMatchExceptionApplicationMapper;
import com.zhs.basket.mapper.BasketMatchExceptionAuditMapper;
import com.zhs.basket.mapper.BasketMatchExceptionMaterialMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.req.MatchExceptionDecideReq;
import com.zhs.basket.service.IBasketMatchOfficialDataCorrectionService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BasketMatchExceptionDuplicateTest {

    private static final Long APPLICATION_ID = 5001L;

    @Mock private BasketMatchExceptionApplicationMapper applicationMapper;
    @Mock private BasketMatchExceptionMaterialMapper materialMapper;
    @Mock private BasketMatchExceptionAuditMapper auditMapper;
    @Mock private BasketMatchMapper matchMapper;
    @Mock private BasketMatchTeamMapper matchTeamMapper;
    @Mock private IBasketMatchValidationService validationService;
    @Mock private IBasketMatchOfficialDataCorrectionService correctionService;
    @Mock private BasketMatchMessageHelper messageHelper;

    @InjectMocks
    private BasketMatchExceptionServiceImpl exceptionService;

    private MockedStatic<SecurityUtils> securityUtils;

    @BeforeEach
    void setUpSecurity() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");
    }

    @AfterEach
    void tearDownSecurity() {
        if (securityUtils != null) {
            securityUtils.close();
        }
    }

    @Test
    void reject_failsWhenAlreadyProcessed() {
        BasketMatchExceptionApplication app = new BasketMatchExceptionApplication();
        app.setId(APPLICATION_ID);
        app.setMatchId(900020001L);
        app.setApplicationStatus("PENDING");
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(app);
        when(applicationMapper.updateDecision(eq(APPLICATION_ID), eq("PENDING"), eq("REJECTED"), any(), any(), any(), any(), any(), any(), anyLong(), any()))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> exceptionService.reject(APPLICATION_ID, "duplicate", "duplicate"));
        assertEquals("异常申请驳回失败，可能已被处理", ex.getMessage());
    }

    @Test
    void decide_keepResult_failsWhenAlreadyProcessed() {
        BasketMatchExceptionApplication app = new BasketMatchExceptionApplication();
        app.setId(APPLICATION_ID);
        app.setMatchId(900020001L);
        app.setApplicationStatus("PENDING");
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(app);

        com.zhs.basket.domain.BasketMatch match = new com.zhs.basket.domain.BasketMatch();
        match.setId(900020001L);
        match.setHomeScore(64);
        match.setAwayScore(55);
        when(validationService.requireMatch(900020001L)).thenReturn(match);
        when(applicationMapper.updateDecision(eq(APPLICATION_ID), eq("PENDING"), eq("APPROVED"), any(), any(), any(), any(), any(), any(), anyLong(), any()))
                .thenReturn(0);

        MatchExceptionDecideReq req = new MatchExceptionDecideReq();
        req.setDecisionType("KEEP_RESULT");
        req.setDecisionResult("keep");

        ServiceException ex = assertThrows(ServiceException.class, () -> exceptionService.decide(APPLICATION_ID, req));
        assertEquals("异常裁定失败，可能已被处理", ex.getMessage());
    }
}
