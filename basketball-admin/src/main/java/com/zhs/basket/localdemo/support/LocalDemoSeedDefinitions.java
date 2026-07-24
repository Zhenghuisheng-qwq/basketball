package com.zhs.basket.localdemo.support;

import com.zhs.basket.localdemo.dto.DemoMatchDto;
import com.zhs.basket.localdemo.dto.DemoPlayerDto;
import com.zhs.basket.localdemo.dto.DemoTeamDto;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 本地演示源数据定义：球队、球员、赛程均为虚构数据，稳定可重复。
 */
public final class LocalDemoSeedDefinitions {

    private LocalDemoSeedDefinitions() {
    }

    public static final String TEAM_CRB = "CRB";
    public static final String TEAM_OBT = "OBT";
    public static final String TEAM_THH = "THH";
    public static final String TEAM_ARD = "ARD";

    public static final BigDecimal[] SCORE_WEIGHTS = {
            new BigDecimal("0.24"),
            new BigDecimal("0.20"),
            new BigDecimal("0.16"),
            new BigDecimal("0.13"),
            new BigDecimal("0.11"),
            new BigDecimal("0.07"),
            new BigDecimal("0.05"),
            new BigDecimal("0.04")
    };

    public static final int[] STARTER_MINUTES = {34, 32, 30, 28, 26};
    public static final int[] BENCH_MINUTES = {18, 16, 16};

    public static List<DemoTeamDto> teams() {
        return List.of(
                DemoTeamDto.builder()
                        .teamNo("LOCAL-DEMO-TEAM-CRB")
                        .teamName("赤焰先锋")
                        .englishName("CRIMSON BLAZERS")
                        .shortName("CRB")
                        .description("以高压防守与快速转换见长的虚构战队，代表暗红与黑色视觉主题。")
                        .build(),
                DemoTeamDto.builder()
                        .teamNo("LOCAL-DEMO-TEAM-OBT")
                        .teamName("黑曜泰坦")
                        .englishName("OBSIDIAN TITANS")
                        .shortName("OBT")
                        .description("强调内线对抗与阵地执行的虚构战队，视觉主题为黑色与深灰。")
                        .build(),
                DemoTeamDto.builder()
                        .teamNo("LOCAL-DEMO-TEAM-THH")
                        .teamName("雷霆猎手")
                        .englishName("THUNDER HUNTERS")
                        .shortName("THH")
                        .description("以外线投射与压迫抢断著称的虚构战队，视觉主题为深蓝与电光灰。")
                        .build(),
                DemoTeamDto.builder()
                        .teamNo("LOCAL-DEMO-TEAM-ARD")
                        .teamName("极光破晓")
                        .englishName("AURORA DAWN")
                        .shortName("ARD")
                        .description("注重团队传导与均衡输出的虚构战队，视觉主题为紫色与暖白。")
                        .build()
        );
    }

    public static Map<String, String> teamKeyByNo() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("LOCAL-DEMO-TEAM-CRB", TEAM_CRB);
        map.put("LOCAL-DEMO-TEAM-OBT", TEAM_OBT);
        map.put("LOCAL-DEMO-TEAM-THH", TEAM_THH);
        map.put("LOCAL-DEMO-TEAM-ARD", TEAM_ARD);
        return map;
    }

    public static List<DemoPlayerDto> players() {
        return List.of(
                player(1, TEAM_CRB, "林彻", "PG", "1", true),
                player(2, TEAM_CRB, "周炎", "SG", "3", false),
                player(3, TEAM_CRB, "陈峻", "SF", "7", false),
                player(4, TEAM_CRB, "赵野", "PF", "11", false),
                player(5, TEAM_CRB, "韩岳", "C", "15", false),
                player(6, TEAM_CRB, "许燃", "G", "21", false),
                player(7, TEAM_CRB, "罗森", "F", "24", false),
                player(8, TEAM_CRB, "唐骁", "C", "33", false),

                player(9, TEAM_OBT, "沈墨", "PG", "1", true),
                player(10, TEAM_OBT, "顾川", "SG", "3", false),
                player(11, TEAM_OBT, "贺岩", "SF", "7", false),
                player(12, TEAM_OBT, "宋毅", "PF", "11", false),
                player(13, TEAM_OBT, "方澈", "C", "15", false),
                player(14, TEAM_OBT, "魏霆", "G", "21", false),
                player(15, TEAM_OBT, "陆沉", "F", "24", false),
                player(16, TEAM_OBT, "蒋峰", "C", "33", false),

                player(17, TEAM_THH, "江雷", "PG", "1", true),
                player(18, TEAM_THH, "谢霄", "SG", "3", false),
                player(19, TEAM_THH, "杜衡", "SF", "7", false),
                player(20, TEAM_THH, "邵风", "PF", "11", false),
                player(21, TEAM_THH, "袁驰", "C", "15", false),
                player(22, TEAM_THH, "彭越", "G", "21", false),
                player(23, TEAM_THH, "郑拓", "F", "24", false),
                player(24, TEAM_THH, "高启", "C", "33", false),

                player(25, TEAM_ARD, "苏曜", "PG", "1", true),
                player(26, TEAM_ARD, "白川", "SG", "3", false),
                player(27, TEAM_ARD, "叶晨", "SF", "7", false),
                player(28, TEAM_ARD, "夏野", "PF", "11", false),
                player(29, TEAM_ARD, "秦北", "C", "15", false),
                player(30, TEAM_ARD, "温岚", "G", "21", false),
                player(31, TEAM_ARD, "孟星", "F", "24", false),
                player(32, TEAM_ARD, "姜远", "C", "33", false)
        );
    }

    public static List<DemoMatchDto> matches() {
        return List.of(
                match(1, TEAM_CRB, TEAM_OBT, 1, 88, 80, -4, true),
                match(2, TEAM_THH, TEAM_ARD, 1, 92, 85, -3, true),
                match(3, TEAM_CRB, TEAM_THH, 2, 78, 81, -2, true),
                match(4, TEAM_OBT, TEAM_ARD, 2, 96, 90, -1, true),
                match(5, TEAM_CRB, TEAM_ARD, 3, 0, 0, 1, false),
                match(6, TEAM_OBT, TEAM_THH, 3, 0, 0, 2, false)
        );
    }

    private static DemoPlayerDto player(int index, String teamKey, String name, String position, String jersey, boolean captain) {
        return DemoPlayerDto.builder()
                .globalIndex(index)
                .teamKey(teamKey)
                .name(name)
                .positionCode(position)
                .jerseyNo(jersey)
                .captain(captain)
                .build();
    }

    private static DemoMatchDto match(int index, String home, String away, int round, int homeScore, int awayScore,
                                      int dayOffset, boolean confirmed) {
        return DemoMatchDto.builder()
                .matchIndex(index)
                .matchNo(String.format("LOCAL-DEMO-MATCH-%03d", index))
                .homeTeamKey(home)
                .awayTeamKey(away)
                .roundNo(round)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .dayOffset(dayOffset)
                .confirmed(confirmed)
                .build();
    }

    public static int[] periodScores(int finalScore, long seed) {
        int[] periods = new int[4];
        int remaining = finalScore;
        java.util.Random random = new java.util.Random(seed);
        for (int i = 0; i < 3; i++) {
            int max = remaining - (3 - i) * 8;
            int min = Math.max(8, remaining - (3 - i) * 28);
            if (max < min) {
                max = min;
            }
            int base = min + random.nextInt(Math.max(1, max - min + 1));
            periods[i] = base;
            remaining -= base;
        }
        periods[3] = remaining;
        return periods;
    }

    public static int[] allocatePlayerPoints(int teamScore, BigDecimal[] weights) {
        int[] points = new int[weights.length];
        int allocated = 0;
        for (int i = 0; i < weights.length; i++) {
            points[i] = weights[i].multiply(BigDecimal.valueOf(teamScore)).intValue();
            allocated += points[i];
        }
        int remainder = teamScore - allocated;
        for (int i = 0; remainder > 0 && i < points.length; i++, remainder--) {
            points[i]++;
        }
        return points;
    }
}
