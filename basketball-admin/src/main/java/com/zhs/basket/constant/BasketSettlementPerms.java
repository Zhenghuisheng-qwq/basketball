package com.zhs.basket.constant;

/**
 * 第六轮数据结算 / 积分榜 / 赛季统计 / 排行榜权限标识。
 */
public final class BasketSettlementPerms {

    private BasketSettlementPerms() {}

    public static final String DATA_SETTLEMENT_LIST = "basket:dataSettlement:list";
    public static final String DATA_SETTLEMENT_QUERY = "basket:dataSettlement:query";
    public static final String DATA_SETTLEMENT_REBUILD = "basket:dataSettlement:rebuild";
    public static final String DATA_SETTLEMENT_CONFIRM = "basket:dataSettlement:confirm";

    public static final String STANDING_LIST = "basket:standing:list";
    public static final String STANDING_QUERY = "basket:standing:query";
    public static final String STANDING_REBUILD = "basket:standing:rebuild";
    public static final String STANDING_EXPORT = "basket:standing:export";

    public static final String TEAM_SEASON_STAT_LIST = "basket:teamSeasonStat:list";
    public static final String TEAM_SEASON_STAT_QUERY = "basket:teamSeasonStat:query";
    public static final String TEAM_SEASON_STAT_REBUILD = "basket:teamSeasonStat:rebuild";
    public static final String TEAM_SEASON_STAT_EXPORT = "basket:teamSeasonStat:export";

    public static final String PLAYER_SEASON_STAT_LIST = "basket:playerSeasonStat:list";
    public static final String PLAYER_SEASON_STAT_QUERY = "basket:playerSeasonStat:query";
    public static final String PLAYER_SEASON_STAT_REBUILD = "basket:playerSeasonStat:rebuild";
    public static final String PLAYER_SEASON_STAT_EXPORT = "basket:playerSeasonStat:export";

    public static final String RANKING_LIST = "basket:ranking:list";
    public static final String RANKING_QUERY = "basket:ranking:query";
    public static final String RANKING_REBUILD = "basket:ranking:rebuild";
    public static final String RANKING_PUBLISH = "basket:ranking:publish";
    public static final String RANKING_EXPORT = "basket:ranking:export";
}
