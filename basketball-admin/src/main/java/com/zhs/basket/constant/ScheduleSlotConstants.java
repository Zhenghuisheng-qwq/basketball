package com.zhs.basket.constant;

/**
 * 赛程时间槽默认配置（可被 basket_schedule_rule 覆盖）。
 */
public final class ScheduleSlotConstants {

    private ScheduleSlotConstants() {
    }

    /** 每个时间段默认最大比赛场次 */
    public static final int DEFAULT_SLOT_CAPACITY = 12;
}
