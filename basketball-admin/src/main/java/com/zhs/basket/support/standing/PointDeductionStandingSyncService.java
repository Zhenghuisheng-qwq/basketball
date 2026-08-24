package com.zhs.basket.support.standing;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.DisciplineTargetTypeEnum;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.service.v4.IBasketV4StatsSettlementService;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PointDeductionStandingSyncService {

    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private StandingStageResolver standingStageResolver;
    @Autowired
    private IBasketV4StatsSettlementService statsSettlementService;

    public void rebuildForRecord(BasketDisciplineRecord record) {
        if (!isPointDeductionEventTeam(record) || record.getEventTeamId() == null) {
            return;
        }
        BasketEventTeam eventTeam = eventTeamMapper.selectById(record.getEventTeamId());
        if (eventTeam == null || Integer.valueOf(1).equals(eventTeam.getIsDelete())) {
            throw new ServiceException("参赛队不存在");
        }
        BasketDivisionStage stage = standingStageResolver.resolveStandingStage(eventTeam.getDivisionId());
        if (stage == null || stage.getId() == null) {
            return;
        }
        statsSettlementService.rebuildStageStats(stage.getId());
    }

    public static boolean isPointDeductionEventTeam(BasketDisciplineRecord record) {
        return record != null
                && DisciplinePenaltyCodeEnum.POINT_DEDUCTION.name().equals(record.getPenaltyCode())
                && DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(record.getTargetType());
    }
}
