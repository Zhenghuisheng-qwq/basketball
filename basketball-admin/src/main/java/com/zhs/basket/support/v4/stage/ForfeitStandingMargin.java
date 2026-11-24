package com.zhs.basket.support.v4.stage;

/**
 * 弃权不写真实比分。积分榜给对手记净胜分，胜方得分 +15、负方失分 +15，净胜分分别是 +15 和 -15。
 */
public final class ForfeitStandingMargin {

    public static final int POINT_MARGIN = 15;

    private ForfeitStandingMargin() {
    }

    /**
     * 球队技术统计只补官方胜负。出战场次和得分仍来自实际交手，净胜分以积分榜为准。
     */
    public static void applyOfficialResult(OfficialResult winner, OfficialResult loser) {
        winner.addOfficialWin();
        loser.addOfficialLoss();
    }

    public interface OfficialResult {
        void addOfficialWin();

        void addOfficialLoss();
    }
}
