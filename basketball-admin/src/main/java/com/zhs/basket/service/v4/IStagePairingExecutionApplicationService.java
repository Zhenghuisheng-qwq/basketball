package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.dto.v4.StageExternalRankingReq;
import com.zhs.basket.dto.v4.StageManualSlotReq;
import com.zhs.basket.dto.v4.StagePairingSeedReq;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import com.zhs.basket.vo.v4.BracketPreviewVo;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;

public interface IStagePairingExecutionApplicationService {

    StagePairingExecutionVo getCurrent(Long stageId);

    BracketPreviewVo previewBracket(Long stageId);

    StagePairingExecutionVo saveAdminSeeds(Long stageId, StagePairingSeedReq req);

    StagePairingExecutionVo saveExternalRanking(Long stageId, StageExternalRankingReq req);

    StagePairingExecutionVo generateRandomDraw(Long stageId);

    StagePairingExecutionVo confirm(Long stageId);

    StagePairingExecutionVo saveManualSlots(Long stageId, StageManualSlotReq req);

    StagePairingExecutionVo generateGroupCross(Long stageId);

    StagePairingExecutionVo resolvePlaceholders(Long stageId);

    StagePairingExecutionVo redraw(Long stageId);

    BracketSlotPlan loadConfirmedPlan(BasketDivisionStage stage);
}
