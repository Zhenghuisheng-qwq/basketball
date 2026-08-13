package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.enums.v4.MatchStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.vo.v4.ScheduleUnitView;
import com.zhs.common.exception.ServiceException;

public final class ScheduleMutableGuard {

    private ScheduleMutableGuard() {}

    public static void assertStructureActive(BasketScheduleBatch batch) {
        if (batch == null) {
            throw new ServiceException("当前赛段没有有效结构，不能排期");
        }
        if (!StructureStatusEnum.ACTIVE.getCode().equals(batch.getStructureStatus())) {
            throw new ServiceException("当前结构已失效，不能修改排期");
        }
    }

    public static void assertNotPublished(BasketScheduleBatch batch) {
        if (batch != null && PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus())) {
            throw new ServiceException("赛段已发布，请先撤回后再修改排期");
        }
    }

    public static void assertCurrentExecution(BasketScheduleBatch batch, Long expectedBatchId) {
        if (batch == null || expectedBatchId == null || !expectedBatchId.equals(batch.getId())) {
            throw new ServiceException("排期单位不属于当前结构版本");
        }
    }

    public static void assertUnitEditable(ScheduleUnitView unit) {
        if (unit == null) {
            throw new ServiceException("排期单位不存在");
        }
        if (!unit.isCanEditSchedule()) {
            throw new ServiceException("当前排期单位不可修改");
        }
        if (MatchStatusEnum.FINISHED.getCode().equals(unit.getMatchStatus())
                || MatchStatusEnum.FORFEITED.getCode().equals(unit.getMatchStatus())
                || "COMPLETED".equals(unit.getSeriesStatus())
                || "FORFEITED".equals(unit.getSeriesStatus())) {
            throw new ServiceException("已产生正式比赛结果的场次不能修改日期时间");
        }
    }
}
