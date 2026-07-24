package com.zhs.basket.localdemo.constant;

/**
 * 本地演示数据统一标识与业务编号前缀。
 */
public final class BasketLocalDemoConstants {

    private BasketLocalDemoConstants() {
    }

    public static final String MARKER = "CNPA_LOCAL_DEMO_V1";
    public static final String BIZ_PREFIX = "LOCAL-DEMO-";
    public static final String REGISTER_SOURCE = "LOCAL_DEMO";
    public static final String GAME_TERMINAL = "PC";
    public static final String ID_TYPE = "OTHER";
    public static final String TOURNAMENT_NO = "LOCAL-DEMO-TOURNAMENT-2026";
    public static final String SEASON_NO = "LOCAL-DEMO-SEASON-2026";
    public static final String STAGE_NO = "LOCAL-DEMO-STAGE-GROUP";
    public static final String GROUP_CODE = "LOCAL-DEMO-GROUP-A";

    /** 16 支战队 × 8 人，4 个小组单循环（每组 6 场，共 24 场；前 2 轮 16 场已确认，第 3 轮 8 场待赛） */
    public static final int TEAM_COUNT = 16;
    public static final int PLAYERS_PER_TEAM = 8;
    public static final int PLAYER_COUNT = TEAM_COUNT * PLAYERS_PER_TEAM;
    public static final int CAPTAIN_COUNT = TEAM_COUNT;
    public static final int GROUP_COUNT = 4;
    public static final int TEAMS_PER_GROUP = 4;
    public static final int ROUND_COUNT = 3;
    public static final int MATCH_COUNT = 24;
    public static final int CONFIRMED_MATCH_COUNT = 16;
    public static final int SCHEDULED_MATCH_COUNT = 8;
    public static final int LINEUP_COUNT = MATCH_COUNT * TEAMS_PER_GROUP * 2;
    public static final int PERIOD_SCORE_COUNT = CONFIRMED_MATCH_COUNT * 4;
    public static final int PLAYER_MATCH_STAT_COUNT = CONFIRMED_MATCH_COUNT * PLAYERS_PER_TEAM * 2;
    public static final int TEAM_MATCH_STAT_COUNT = CONFIRMED_MATCH_COUNT * 2;
    public static final int STANDING_MIN_COUNT = TEAM_COUNT;
    public static final int TEAM_SEASON_STAT_MIN = TEAM_COUNT;
    public static final int PLAYER_SEASON_STAT_MIN = PLAYER_COUNT;

    /** basketball-web/src/main/resources/player 下默认球员头像数量（1.png ~ 20.png） */
    public static final int PLAYER_AVATAR_COUNT = 20;

    /** basketball-web/src/main/resources/head 下默认球队 Logo 数量（1.png ~ 18.png） */
    public static final int TEAM_LOGO_COUNT = 18;

    public static String playerAvatarObjectKey(int avatarNo) {
        return String.format("LOCAL-DEMO-PLAYER-%02d", avatarNo);
    }

    public static String playerAvatarAccessUrl(int avatarNo) {
        // 避免与官网路由 /player/:playerId 冲突
        return "/player-avatar/" + avatarNo + ".png";
    }

    public static int resolvePlayerAvatarNo(int playerIndex) {
        return ((playerIndex - 1) % PLAYER_AVATAR_COUNT) + 1;
    }

    public static String teamLogoObjectKey(int logoNo) {
        return String.format("LOCAL-DEMO-HEAD-%02d", logoNo);
    }

    public static String teamLogoAccessUrl(int logoNo) {
        return "/head/" + logoNo + ".png";
    }

    public static int resolveTeamLogoNo(int teamIndex) {
        return ((teamIndex - 1) % TEAM_LOGO_COUNT) + 1;
    }

    public static String groupCode(String groupKey) {
        return "LOCAL-DEMO-GROUP-" + groupKey;
    }

    public static String demoEmail(int index) {
        return String.format("demo.player%03d@example.com", index);
    }

    public static String demoUsername(int index) {
        return String.format("demo-player-%03d", index);
    }

    public static String accountNo(int index) {
        return String.format("LOCAL-DEMO-ACCOUNT-%03d", index);
    }

    public static String playerNo(int index) {
        return String.format("LOCAL-DEMO-P%03d", index);
    }

    public static String certApplicationNo(int index) {
        return String.format("LOCAL-DEMO-CERT-%03d", index);
    }

    public static String matchNo(int index) {
        return String.format("LOCAL-DEMO-MATCH-%03d", index);
    }

    public static String announcementNo(int index) {
        return String.format("LOCAL-DEMO-ANN-%03d", index);
    }

    public static String bannerName(int index) {
        return String.format("LOCAL-DEMO-BANNER-%03d", index);
    }

    public static String demoMobile(int index) {
        return String.format("199%08d", 10000000 + index);
    }

    public static String demoIdNo(int index) {
        return String.format("LOCALDEMO%06d", index);
    }
}
