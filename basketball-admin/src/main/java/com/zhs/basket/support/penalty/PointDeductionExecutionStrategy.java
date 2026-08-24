package com.zhs.basket.support.penalty;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.PenaltyExecutionStatusEnum;
import com.zhs.basket.support.standing.PointDeductionStandingSyncService;
import com.zhs.basket.vo.PenaltyExecutionResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class PointDeductionExecutionStrategy implements PenaltyExecutionStrategy {

    @Autowired
    private PointDeductionStandingSyncService standingSyncService;

    @Override
    public boolean supports(BasketDisciplineRecord record) {
        return PointDeductionStandingSyncService.isPointDeductionEventTeam(record)
                && DisciplinePenaltyCodeEnum.POINT_DEDUCTION.name().equals(record.getPenaltyCode());
    }

    @Override
    public PenaltyExecutionResultVo execute(BasketDisciplineRecord record) {
        standingSyncService.rebuildForRecord(record);
        PenaltyExecutionResultVo result = new PenaltyExecutionResultVo();
        result.setExecutionStatus(PenaltyExecutionStatusEnum.EFFECTIVE.name());
        result.setExecutedTime(new Date());
        result.setEventTeamId(record.getEventTeamId());
        return result;
    }

    @Override
    public void restoreEligibility(BasketDisciplineRecord record) {
        standingSyncService.rebuildForRecord(record);
    }

    @Override
    public void syncAfterEffective(BasketDisciplineRecord record) {
        standingSyncService.rebuildForRecord(record);
    }
}
