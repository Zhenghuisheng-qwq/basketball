package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchDataDispute;
import com.zhs.basket.domain.BasketMatchOfficialResult;
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
import com.zhs.common.utils.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BasketMatchDisputeWindowTest {

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

    private MockedStatic<SecurityUtils> securityUtils;
    private BasketMatch match;
    private MatchDisputeCreateReq req;

    @BeforeEach
    void setUp() {
        securityUtils = mockStatic(SecurityUtils.class);
        securityUtils.when(SecurityUtils::getUserId).thenReturn(1L);
        securityUtils.when(SecurityUtils::getUsername).thenReturn("admin");

        match = new BasketMatch();
        match.setId(MATCH_ID);
        match.setDataStatus(MatchDataStatusEnum.CONFIRMED.getCode());
        match.setUpdateTime(new Date());

        req = new MatchDisputeCreateReq();
        req.setMatchId(MATCH_ID);
        req.setDisputeType("PLAYER_STAT");
        req.setDisputeReason("assist count wrong");
    }

    @AfterEach
    void tearDown() {
        if (securityUtils != null) {
            securityUtils.close();
        }
    }

    @Test
    void create_rejectsWhenDisputeWindowExpired() {
        when(validationService.requireMatch(MATCH_ID)).thenReturn(match);
        when(disputeMapper.countActiveByMatchId(MATCH_ID)).thenReturn(0);

        BasketMatchOfficialResult official = new BasketMatchOfficialResult();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -13);
        official.setEffectiveTime(cal.getTime());
        when(officialResultMapper.selectCurrentByMatchId(MATCH_ID)).thenReturn(official);

        ServiceException ex = assertThrows(ServiceException.class, () -> disputeService.create(req));
        assertEquals("争议窗口已结束，请联系管理员", ex.getMessage());
    }

    @Test
    void create_allowsWithinTwelveHourWindow() {
        when(validationService.requireMatch(MATCH_ID)).thenReturn(match);
        when(disputeMapper.countActiveByMatchId(MATCH_ID)).thenReturn(0);

        BasketMatchOfficialResult official = new BasketMatchOfficialResult();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -1);
        official.setEffectiveTime(cal.getTime());
        when(officialResultMapper.selectCurrentByMatchId(MATCH_ID)).thenReturn(official);
        when(disputeMapper.insert(any(BasketMatchDataDispute.class))).thenAnswer(inv -> {
            BasketMatchDataDispute row = inv.getArgument(0);
            row.setId(100L);
            return 1;
        });

        Long id = disputeService.create(req);
        assertEquals(100L, id);
    }
}
