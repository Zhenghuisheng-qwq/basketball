package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.mapper.BasketMatchDataDisputeMapper;
import com.zhs.basket.mapper.BasketMatchDataDisputeMaterialMapper;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchOfficialResultMapper;
import com.zhs.basket.req.MatchDisputeCreateReq;
import com.zhs.basket.service.IBasketMatchOfficialDataCorrectionService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchDisputeDuplicateTest {

    private static final Long MATCH_ID = 900020001L;

    @Mock private IBasketMatchValidationService validationService;
    @Mock private BasketMatchMapper matchMapper;
    @Mock private BasketMatchDataDisputeMapper disputeMapper;
    @Mock private BasketMatchDataDisputeMaterialMapper disputeMaterialMapper;
    @Mock private BasketMatchOfficialResultMapper officialResultMapper;
    @Mock private IBasketMatchOfficialDataCorrectionService correctionService;
    @Mock private BasketMatchMessageHelper messageHelper;

    @InjectMocks
    private BasketMatchDisputeServiceImpl disputeService;

    private MatchDisputeCreateReq req;

    @BeforeEach
    void setUp() {
        req = new MatchDisputeCreateReq();
        req.setMatchId(MATCH_ID);
        req.setDisputeType("SCORE");
        req.setDisputeReason("duplicate test");
    }

    @Test
    void create_rejectsWhenActiveDisputeExists() {
        BasketMatch match = new BasketMatch();
        match.setId(MATCH_ID);
        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        when(validationService.requireMatch(MATCH_ID)).thenReturn(match);
        when(disputeMapper.countActiveByMatchId(MATCH_ID)).thenReturn(1);

        ServiceException ex = assertThrows(ServiceException.class, () -> disputeService.create(req));
        assertEquals("当前比赛已有待处理争议", ex.getMessage());
    }
}
