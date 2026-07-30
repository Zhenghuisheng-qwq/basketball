package com.zhs.basket.constant;

/**
 * CNPA 正式参赛阵容人数规则：每队 5–8 人（固定 5 首发，其余为替补）。
 */
public final class CnpaRosterConstants {

    private CnpaRosterConstants() {
    }

    public static final int MIN_ROSTER_COUNT = 5;
    public static final int MAX_ROSTER_COUNT = 8;
    public static final int REQUIRED_STARTING_COUNT = 5;
    public static final int TEAM_MAX_MEMBER_COUNT = 8;
}
