package com.zhs.basket.support.v4.stage;

/**
 * 弃权不写真实比分。积分榜给对手记净胜分，胜方得分 +15、负方失分 +15，净胜分分别是 +15 和 -15。
 */
public final class ForfeitStandingMargin {

    public static final int POINT_MARGIN = 15;

    private ForfeitStandingMargin() {
    }
}
