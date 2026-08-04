package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketStageManualRankReq;
import com.zhs.basket.support.v4.stage.StageSchedulePreviewVo;
import com.zhs.basket.vo.v4.BasketDivisionStageExecutionListVo;
import com.zhs.basket.vo.v4.BasketStageQualificationPreviewVo;
import com.zhs.basket.vo.v4.BasketStageStandingScopeVo;

import java.util.List;

/**
 * Division 阶段执行业务服务。
 */
public interface IDivisionStageExecutionApplicationService {

    List<BasketDivisionStageExecutionListVo> listStages(Long divisionId);

    void initializeStageTeams(Long stageId);

    StageSchedulePreviewVo previewSchedule(Long stageId);

    Long generateSchedule(Long stageId);

    void publishSchedule(Long stageId);

    void cancelDraftSchedule(Long stageId);

    void checkStageCompletion(Long stageId);

    BasketStageQualificationPreviewVo previewQualification(Long stageId);

    void confirmQualification(Long stageId);

    void cancelQualificationConfirm(Long stageId);

    List<BasketStageStandingScopeVo> listStandings(Long stageId);

    void applyManualRank(BasketStageManualRankReq req);
}
