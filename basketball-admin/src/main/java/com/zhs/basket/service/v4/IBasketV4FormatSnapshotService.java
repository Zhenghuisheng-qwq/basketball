package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketDivisionFormatPlanApplyReq;
import com.zhs.basket.dto.v4.BasketDivisionStageOrderReq;
import com.zhs.basket.dto.v4.BasketDivisionStageSaveReq;
import com.zhs.basket.dto.v4.BasketDivisionTieBreakRuleSaveReq;
import com.zhs.basket.dto.v4.StandardTemplateApplyReq;
import com.zhs.basket.vo.v4.BasketDivisionFormatSnapshotVo;
import com.zhs.basket.vo.v4.FormatPlanValidationResultVo;
import com.zhs.basket.vo.v4.FormatRevisionPreviewVo;

public interface IBasketV4FormatSnapshotService {
    Long createDraftSnapshot(Long divisionId, Long templateId);

    void rebuildDraftSnapshot(Long divisionId, Long templateId);

    BasketDivisionFormatSnapshotVo getDivisionSnapshot(Long divisionId);

    void validateSnapshotReady(Long divisionId);

    void lockSnapshot(Long divisionId, Long operatorId);

    Long addStage(Long divisionId, BasketDivisionStageSaveReq req);

    void updateStage(Long divisionId, Long stageId, BasketDivisionStageSaveReq req);

    void deleteStage(Long divisionId, Long stageId);

    void reorderStages(Long divisionId, BasketDivisionStageOrderReq req);

    void saveTieBreakRules(Long divisionId, BasketDivisionTieBreakRuleSaveReq req);

    FormatPlanValidationResultVo validateFormatPlan(BasketDivisionFormatPlanApplyReq req);

    BasketDivisionFormatSnapshotVo applyFormatPlan(Long divisionId, BasketDivisionFormatPlanApplyReq req);

    BasketDivisionFormatSnapshotVo applyStandardTemplate(Long divisionId, StandardTemplateApplyReq req);

    FormatRevisionPreviewVo previewPublishedRevision(Long divisionId, StandardTemplateApplyReq req);

    BasketDivisionFormatSnapshotVo publishPublishedRevision(Long divisionId, StandardTemplateApplyReq req, Long operatorId);
}
