package com.zhs.basket.service.v4;

import com.zhs.basket.dto.v4.StageWithdrawReq;
import com.zhs.basket.vo.v4.StagePublishPreviewVo;
import com.zhs.basket.vo.v4.StagePublishRecordVo;

import java.util.List;

public interface IStagePublishApplicationService {

    StagePublishPreviewVo preview(Long stageId);

    StagePublishRecordVo publish(Long stageId);

    StagePublishRecordVo withdraw(Long stageId, StageWithdrawReq req);

    StagePublishRecordVo republish(Long stageId);

    List<StagePublishRecordVo> listRecords(Long stageId);
}
