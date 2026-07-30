package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.mapper.BasketMatchDataDisputeMapper;
import com.zhs.basket.mapper.BasketMatchDataDisputeMaterialMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchOfficialResultMapper;
import com.zhs.basket.req.MatchDisputeCorrectionReq;
import com.zhs.basket.req.MatchDisputeHandleReq;
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
class BasketMatchDisputeUpholdIdempotentTest {

    private static final Long DISPUTE_ID = 3001L;

    @Mock private IBasketMatchValidationService validationService;
    @Mock private BasketMatchMapper matchMapper;
    @Mock private BasketMatchDataDisputeMapper disputeMapper;
    @Mock private BasketMatchDataDisputeMaterialMapper disputeMaterialMapper;
    @Mock private BasketMatchOfficialResultMapper officialResultMapper;
    @Mock private IBasketMatchOfficialDataCorrectionService correctionService;
    @Mock private BasketMatchMessageHelper messageHelper;

    @InjectMocks
    private BasketMatchDisputeServiceImpl disputeService;

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
    void upholdAndCorrect_rejectsWhenNotPending() {
        BasketMatchDataDispute dispute = new BasketMatchDataDispute();
        dispute.setId(DISPUTE_ID);
        dispute.setMatchId(900020001L);
        dispute.setDisputeStatus("UPHELD");
        when(disputeMapper.selectById(DISPUTE_ID)).thenReturn(dispute);

        MatchDisputeCorrectionReq req = new MatchDisputeCorrectionReq();
        req.setHandleRemark("fix assists");

        ServiceException ex = assertThrows(ServiceException.class, () -> disputeService.upholdAndCorrect(DISPUTE_ID, req));
        assertEquals("争议状态不可修正", ex.getMessage());
    }

    @Test
    void uphold_failsWhenConcurrentHandle() {
        BasketMatchDataDispute dispute = new BasketMatchDataDispute();
        dispute.setId(DISPUTE_ID);
        dispute.setMatchId(900020001L);
        dispute.setDisputeStatus("PENDING");
        when(disputeMapper.updateHandle(eq(DISPUTE_ID), eq("PENDING"), eq("UPHELD"), anyLong(), any(), any(), any(), isNull(), isNull()))
                .thenReturn(0);

        MatchDisputeHandleReq req = new MatchDisputeHandleReq();
        req.setHandleRemark("accepted");

        ServiceException ex = assertThrows(ServiceException.class, () -> disputeService.uphold(DISPUTE_ID, req));
        assertEquals("争议处理失败，可能已被其他管理员处理", ex.getMessage());
    }

    @Test
    void upholdAndCorrect_failsWhenConcurrentHandleAfterCorrection() {
        BasketMatchDataDispute dispute = new BasketMatchDataDispute();
        dispute.setId(DISPUTE_ID);
        dispute.setMatchId(900020001L);
        dispute.setDisputeStatus("PENDING");
        when(disputeMapper.selectById(DISPUTE_ID)).thenReturn(dispute);
        when(correctionService.applyCorrection(eq(900020001L), any(), any(), anyLong(), eq(DISPUTE_ID), any()))
                .thenReturn(9001L);
        when(disputeMapper.updateHandle(eq(DISPUTE_ID), eq("PENDING"), eq("UPHELD"), anyLong(), any(), any(), any(), isNull(), eq(9001L)))
                .thenReturn(0);

        MatchDisputeCorrectionReq req = new MatchDisputeCorrectionReq();
        req.setHandleRemark("fix assists");

        ServiceException ex = assertThrows(ServiceException.class, () -> disputeService.upholdAndCorrect(DISPUTE_ID, req));
        assertEquals("争议采纳失败，可能已被其他管理员处理", ex.getMessage());
    }
}
