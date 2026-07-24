package com.zhs.basket.constant;

/**
 * 比赛模块常量（判胜默认比分 20-0）
 */
public final class BasketMatchConstants {

    private BasketMatchConstants() {
    }

    public static final int STARTER_COUNT = 5;
    public static final int CAPTAIN_COUNT = 1;
    /** 判胜默认得分 */
    public static final int WALKOVER_WIN_SCORE = 20;
    /** 判负默认得分 */
    public static final int WALKOVER_LOSS_SCORE = 0;
    public static final int REGISTRATION_APPROVED_STATUS = 3;
    public static final int REGISTRATION_PLAYER_PASS = 1;
    /** 无结束时间时冲突检测默认时长（毫秒）：2小时 */
    public static final long DEFAULT_MATCH_DURATION_MS = 2L * 60 * 60 * 1000;
}
