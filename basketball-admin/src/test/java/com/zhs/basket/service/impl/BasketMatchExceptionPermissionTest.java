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
import com.zhs.basket.domain.BasketMatch;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchExceptionPermissionTest {

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
    private MatchExceptionDecideReq req;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

        req = new MatchExceptionDecideReq();
        req.setDecisionType("FORFEIT");
        req.setDecisionResult("home forfeit");
    }

    @AfterEach
    void tearDown() {
        if (securityUtils != null) {
            securityUtils.close();
        }
    }

    @Test
    void decide_rejectsForfeitWithoutWalkoverTeam() {
        BasketMatchExceptionApplication app = pendingApplication();
        when(applicationMapper.selectById(APPLICATION_ID)).thenReturn(app);
        when(validationService.requireMatch(app.getMatchId())).thenReturn(new BasketMatch());

        ServiceException ex = assertThrows(ServiceException.class, () -> exceptionService.decide(APPLICATION_ID, req));
        assertEquals("弃赛方不能为空", ex.getMessage());
    }

    @Test
    void reject_failsWhenApplicationNotFound() {
        when(applicationMapper.selectById(anyLong())).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> exceptionService.reject(APPLICATION_ID, "reason", "remark"));
        assertEquals("异常申请不存在", ex.getMessage());
    }

    private BasketMatchExceptionApplication pendingApplication() {
        BasketMatchExceptionApplication app = new BasketMatchExceptionApplication();
        app.setId(APPLICATION_ID);
        app.setMatchId(900020001L);
        app.setApplicationStatus("PENDING");
        app.setApplicantTeamId(900010001L);
        app.setOpponentTeamId(900010002L);
        return app;
    }
}
