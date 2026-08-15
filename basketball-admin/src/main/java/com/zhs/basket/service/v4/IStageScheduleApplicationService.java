package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.ScheduleAssignReq;
import com.zhs.basket.dto.v4.ScheduleBatchAssignReq;
import com.zhs.basket.dto.v4.ScheduleRecommendApplyReq;
import com.zhs.basket.dto.v4.ScheduleRescheduleReq;
import com.zhs.basket.vo.v4.ScheduleRecommendationPreviewVo;
import com.zhs.basket.vo.v4.StagePublishRecordVo;
import com.zhs.basket.vo.v4.StageScheduleBoardVo;
import com.zhs.basket.vo.v4.StageScheduleSummaryVo;

public interface IStageScheduleApplicationService {

    StageScheduleBoardVo listScheduleUnits(Long stageId, String scheduleStatus);

    StageScheduleSummaryVo summary(Long stageId);

    void assign(Long stageId, ScheduleAssignReq req);

    void batchAssign(Long stageId, ScheduleBatchAssignReq req);

    void clear(Long stageId, String unitType, Long unitId);

    ScheduleRecommendationPreviewVo previewRecommend(Long stageId, boolean overwriteScheduled);

    void applyRecommend(Long stageId, ScheduleRecommendApplyReq req);

    StagePublishRecordVo reschedule(Long stageId, ScheduleRescheduleReq req);
}
