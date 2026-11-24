package com.zhs.basket.service.v4;

import com.zhs.basket.vo.v4.StagePairingExecutionVo;

public interface IPlaceholderResolutionApplicationService {

    StagePairingExecutionVo resolve(Long knockoutStageId);

    /**
     * 小组赛晋级刚确认、且淘汰赛签表已经发布时，把 GROUP_RANK 写成真实球队。
     * 签表还没确认时不做事，留给稍后的「解析占位」。
     */
    StagePairingExecutionVo resolveAfterQualificationConfirm(Long knockoutStageId);
}
