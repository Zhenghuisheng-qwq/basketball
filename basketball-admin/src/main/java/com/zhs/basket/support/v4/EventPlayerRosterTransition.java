package com.zhs.basket.support.v4;

/**
 * 赛中名单变更对 EventPlayer 的动作。
 * 已被比赛统计引用的行只改当前状态，不删除、不改身份，也不覆盖昵称快照。
 */
public final class EventPlayerRosterTransition {

    public static final String ACTIVE = "ACTIVE";
    public static final String RETIRED = "RETIRED";

    public enum Action {
        INSERT,
        ACTIVATE_KEEP_SNAPSHOT,
        ACTIVATE_REFRESH,
        RETIRE,
        DELETE,
        NONE
    }

    private EventPlayerRosterTransition() {
    }

    public static Action decide(boolean desired, boolean exists, boolean deleted, boolean referenced) {
        if (desired) {
            if (!exists) {
                return Action.INSERT;
            }
            return referenced ? Action.ACTIVATE_KEEP_SNAPSHOT : Action.ACTIVATE_REFRESH;
        }
        if (!exists || deleted) {
            return Action.NONE;
        }
        return referenced ? Action.RETIRE : Action.DELETE;
    }
}
