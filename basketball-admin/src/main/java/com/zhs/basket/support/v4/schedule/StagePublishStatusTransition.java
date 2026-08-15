package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.StagePublishActionTypeEnum;
import com.zhs.common.exception.ServiceException;

public final class StagePublishStatusTransition {

    private StagePublishStatusTransition() {}

    public static PublishStatusEnum next(String currentCode, StagePublishActionTypeEnum action) {
        PublishStatusEnum current = currentCode == null || currentCode.isEmpty()
                ? PublishStatusEnum.DRAFT
                : PublishStatusEnum.fromCode(currentCode);
        return next(current, action);
    }

    public static PublishStatusEnum next(PublishStatusEnum current, StagePublishActionTypeEnum action) {
        if (action == StagePublishActionTypeEnum.PUBLISH) {
            if (current == PublishStatusEnum.PUBLISHED) {
                return PublishStatusEnum.PUBLISHED;
            }
            if (current == PublishStatusEnum.DRAFT || current == PublishStatusEnum.WITHDRAWN) {
                return PublishStatusEnum.PUBLISHED;
            }
            throw new ServiceException("当前发布状态不允许发布: " + current.getCode());
        }
        if (action == StagePublishActionTypeEnum.REPUBLISH) {
            if (current == PublishStatusEnum.WITHDRAWN) {
                return PublishStatusEnum.PUBLISHED;
            }
            throw new ServiceException("仅已撤回赛段可以再发布");
        }
        if (action == StagePublishActionTypeEnum.WITHDRAW) {
            if (current == PublishStatusEnum.PUBLISHED) {
                return PublishStatusEnum.WITHDRAWN;
            }
            throw new ServiceException("仅已发布赛段可以撤回");
        }
        if (action == StagePublishActionTypeEnum.SCHEDULE_ADJUST) {
            if (current == PublishStatusEnum.PUBLISHED) {
                return PublishStatusEnum.PUBLISHED;
            }
            throw new ServiceException(ScheduleAdjustmentGuard.MSG_NOT_PUBLISHED);
        }
        throw new ServiceException("未知发布动作");
    }

    public static boolean isPublished(String code) {
        return PublishStatusEnum.PUBLISHED.getCode().equals(code);
    }
}
