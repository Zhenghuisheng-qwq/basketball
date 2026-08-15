package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.BasketStandingPointAdjustmentReq;
import com.zhs.basket.vo.v4.BasketStandingPointAdjustmentVo;

import java.util.List;

public interface IStandingPointAdjustmentApplicationService {

    BasketStandingPointAdjustmentVo applyAdjustment(BasketStandingPointAdjustmentReq req);

    List<BasketStandingPointAdjustmentVo> listHistory(Long divisionId, Long stageId, Long eventTeamId);
}
