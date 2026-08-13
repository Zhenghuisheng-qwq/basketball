package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.StagePairingExecutionVo;

public interface IPlaceholderResolutionApplicationService {

    StagePairingExecutionVo resolve(Long knockoutStageId);
}
