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

    public static String demoEmail(int index) {
        return String.format("demo.player%02d@example.com", index);
    }

    public static String demoMobile(int index) {
        return String.format("199%08d", 10000000 + index);
    }

    public static String demoIdNo(int index) {
        return String.format("LOCALDEMO%06d", index);
    }
}
