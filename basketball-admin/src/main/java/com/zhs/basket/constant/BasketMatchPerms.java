package com.zhs.basket.constant;

/**
 * Round 5 比赛执行模块权限标识
 */
public final class BasketMatchPerms {

    private BasketMatchPerms() {
    }

    public static final String STAGE_LIST = "basket:stage:list";
    public static final String STAGE_QUERY = "basket:stage:query";
    public static final String STAGE_ADD = "basket:stage:add";
    public static final String STAGE_EDIT = "basket:stage:edit";
    public static final String STAGE_REMOVE = "basket:stage:remove";
    public static final String STAGE_STATUS = "basket:stage:status";

    public static final String ROUND_LIST = "basket:round:list";
    public static final String ROUND_QUERY = "basket:round:query";
    public static final String ROUND_ADD = "basket:round:add";
    public static final String ROUND_EDIT = "basket:round:edit";
    public static final String ROUND_REMOVE = "basket:round:remove";
    public static final String ROUND_STATUS = "basket:round:status";

    public static final String GROUP_LIST = "basket:group:list";
    public static final String GROUP_QUERY = "basket:group:query";
    public static final String GROUP_ADD = "basket:group:add";
    public static final String GROUP_EDIT = "basket:group:edit";
    public static final String GROUP_REMOVE = "basket:group:remove";
    public static final String GROUP_ASSIGN_TEAM = "basket:group:assignTeam";

    public static final String MATCH_LIST = "basket:match:list";
    public static final String MATCH_QUERY = "basket:match:query";
    public static final String MATCH_ADD = "basket:match:add";
    public static final String MATCH_EDIT = "basket:match:edit";
    public static final String MATCH_SCHEDULE = "basket:match:schedule";
    public static final String MATCH_POSTPONE = "basket:match:postpone";
    public static final String MATCH_CANCEL = "basket:match:cancel";
    public static final String MATCH_START = "basket:match:start";
    public static final String MATCH_REMOVE = "basket:match:remove";

    public static final String LINEUP_LIST = "basket:lineup:list";
    public static final String LINEUP_QUERY = "basket:lineup:query";
    public static final String LINEUP_EDIT = "basket:lineup:edit";
    public static final String LINEUP_CONFIRM = "basket:lineup:confirm";

    public static final String SCORE_QUERY = "basket:score:query";
    public static final String SCORE_EDIT = "basket:score:edit";
    public static final String SCORE_SUBMIT = "basket:score:submit";
    public static final String SCORE_CONFIRM = "basket:score:confirm";
    public static final String SCORE_REOPEN = "basket:score:reopen";

    public static final String MATCH_STAT_QUERY = "basket:matchStat:query";
    public static final String MATCH_STAT_EDIT = "basket:matchStat:edit";
    public static final String MATCH_STAT_SUBMIT = "basket:matchStat:submit";
    public static final String MATCH_STAT_CONFIRM = "basket:matchStat:confirm";
    public static final String MATCH_STAT_REOPEN = "basket:matchStat:reopen";
    public static final String MATCH_STAT_AUDIT = "basket:matchStat:audit";
}
