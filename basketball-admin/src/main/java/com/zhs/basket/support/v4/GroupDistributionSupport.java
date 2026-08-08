package com.zhs.basket.support.v4;

import java.util.ArrayList;
import java.util.List;

/**
 * 将 N 支球队均衡分配到 G 个小组，各组人数差最多 1（如 14 队 4 组 → 4/4/3/3）。
 */
public final class GroupDistributionSupport {

    private GroupDistributionSupport() {
    }

    public static List<Integer> evenGroupSizes(int teamCount, int groupCount) {
        if (teamCount <= 0 || groupCount <= 0) {
            throw new IllegalArgumentException("teamCount 与 groupCount 必须为正整数");
        }
        int base = teamCount / groupCount;
        int remainder = teamCount % groupCount;
        List<Integer> sizes = new ArrayList<>(groupCount);
        for (int i = 0; i < groupCount; i++) {
            sizes.add(base + (i < remainder ? 1 : 0));
        }
        return sizes;
    }
}
