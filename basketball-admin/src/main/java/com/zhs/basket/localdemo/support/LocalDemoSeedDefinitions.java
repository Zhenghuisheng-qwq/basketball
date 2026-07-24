package com.zhs.basket.localdemo.support;

import com.zhs.basket.localdemo.constant.BasketLocalDemoConstants;
import com.zhs.basket.localdemo.dto.DemoMatchDto;
import com.zhs.basket.localdemo.dto.DemoPlayerDto;
import com.zhs.basket.localdemo.dto.DemoTeamDto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 本地演示源数据定义：16 支虚构球队、128 名球员、24 场小组循环赛（4 组 × 6 场）。
 */
public final class LocalDemoSeedDefinitions {

    private LocalDemoSeedDefinitions() {
    }

    public static final String TEAM_CRB = "CRB";
    public static final String TEAM_OBT = "OBT";
    public static final String TEAM_THH = "THH";
    public static final String TEAM_ARD = "ARD";

    public static final String[] GROUP_KEYS = {"A", "B", "C", "D"};

    /** 四队单循环三轮对阵（索引为组内 0-3） */
    private static final int[][][] ROUND_ROBIN = {
            {{0, 3}, {1, 2}},
            {{0, 2}, {1, 3}},
            {{0, 1}, {2, 3}}
    };

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

    private static final String[] NAME_GIVEN = {
            "林", "周", "陈", "赵", "韩", "许", "罗", "唐", "沈", "顾", "贺", "宋",
            "方", "魏", "陆", "蒋", "江", "谢", "杜", "邵", "袁", "彭", "郑", "高",
            "苏", "白", "叶", "夏", "秦", "温", "孟", "姜", "萧", "程", "傅", "卢",
            "丁", "任", "姚", "谭", "黎", "金", "石", "段", "龙", "郝", "孔", "严"
    };

    private static final String[] NAME_SUR = {
            "彻", "炎", "峻", "野", "岳", "燃", "森", "骁", "墨", "川", "岩", "毅",
            "澈", "霆", "沉", "峰", "雷", "霄", "衡", "风", "驰", "越", "拓", "启",
            "曜", "川", "晨", "野", "北", "岚", "星", "远", "寒", "扬", "宁", "航",
            "泽", "言", "清", "逸", "铭", "博", "凯", "诚", "瑞", "哲", "昊", "然"
    };

    private static final String[] POSITIONS = {"PG", "SG", "SF", "PF", "C", "G", "F", "C/F"};
    private static final String[] JERSEYS = {"1", "3", "7", "11", "15", "21", "24", "33"};

    public static List<DemoTeamDto> teams() {
        return List.of(
                team("A", "LOCAL-DEMO-TEAM-CRB", "赤焰先锋", "CRIMSON BLAZERS", TEAM_CRB,
                        "以高压防守与快速转换见长的虚构战队，代表暗红与黑色视觉主题。"),
                team("A", "LOCAL-DEMO-TEAM-OBT", "黑曜泰坦", "OBSIDIAN TITANS", TEAM_OBT,
                        "强调内线对抗与阵地执行的虚构战队，视觉主题为黑色与深灰。"),
                team("A", "LOCAL-DEMO-TEAM-THH", "雷霆猎手", "THUNDER HUNTERS", TEAM_THH,
                        "以外线投射与压迫抢断著称的虚构战队，视觉主题为深蓝与电光灰。"),
                team("A", "LOCAL-DEMO-TEAM-ARD", "极光破晓", "AURORA DAWN", TEAM_ARD,
                        "注重团队传导与均衡输出的虚构战队，视觉主题为紫色与暖白。"),

                team("B", "LOCAL-DEMO-TEAM-FLM", "焰锋联盟", "FLAME LEGION", "FLM",
                        "擅长外线火力的虚构战队，视觉主题为橙红与炭黑。"),
                team("B", "LOCAL-DEMO-TEAM-STG", "钢铁壁垒", "STEEL BASTION", "STG",
                        "以内线肉搏和篮板控制见长的虚构战队，视觉主题为银灰与深黑。"),
                team("B", "LOCAL-DEMO-TEAM-VPR", "毒蛇梯队", "VIPER UNIT", "VPR",
                        "强调压迫防守与快速反击的虚构战队，视觉主题为墨绿与黑色。"),
                team("B", "LOCAL-DEMO-TEAM-NVS", "夜航先锋", "NIGHT VOYAGER", "NVS",
                        "以节奏控制与挡拆配合见长的虚构战队，视觉主题为藏青与银白。"),

                team("C", "LOCAL-DEMO-TEAM-BLZ", "蓝焰冲锋", "BLUE CHARGE", "BLZ",
                        "注重攻防转换速度的虚构战队，视觉主题为亮蓝与白色。"),
                team("C", "LOCAL-DEMO-TEAM-GRN", "深林猎手", "FOREST HUNTER", "GRN",
                        "以侧翼突破与协防见长的虚构战队，视觉主题为深绿与土褐。"),
                team("C", "LOCAL-DEMO-TEAM-SLV", "银翼突击", "SILVER ASSAULT", "SLV",
                        "强调中距离投射的虚构战队，视觉主题为银白与冰蓝。"),
                team("C", "LOCAL-DEMO-TEAM-CRN", "皇冠骑士", "CROWN KNIGHTS", "CRN",
                        "以团队纪律和执行力著称的虚构战队，视觉主题为金紫与深灰。"),

                team("D", "LOCAL-DEMO-TEAM-GLD", "金潮战队", "GOLDEN SURGE", "GLD",
                        "擅长阵地进攻的虚构战队，视觉主题为金色与墨绿。"),
                team("D", "LOCAL-DEMO-TEAM-FRZ", "霜峰守卫", "FROST GUARD", "FRZ",
                        "以防守轮转和三分投射见长的虚构战队，视觉主题为冰蓝与白色。"),
                team("D", "LOCAL-DEMO-TEAM-EMB", "余烬幻影", "EMBER PHANTOM", "EMB",
                        "强调无球跑动与空切得分的虚构战队，视觉主题为橙褐与深灰。"),
                team("D", "LOCAL-DEMO-TEAM-SKY", "苍穹之击", "SKY STRIKER", "SKY",
                        "以外线群射与快攻见长的虚构战队，视觉主题为天蓝与暖白。")
        );
    }

    public static Map<String, String> teamKeyByNo() {
        Map<String, String> map = new LinkedHashMap<>();
        for (DemoTeamDto team : teams()) {
            map.put(team.getTeamNo(), team.getShortName());
        }
        return map;
    }

    public static List<String> teamKeysInGroup(String groupKey) {
        List<String> keys = new ArrayList<>();
        for (DemoTeamDto team : teams()) {
            if (groupKey.equals(team.getGroupKey())) {
                keys.add(team.getShortName());
            }
        }
        return keys;
    }

    public static List<DemoPlayerDto> players() {
        List<DemoPlayerDto> list = new ArrayList<>();
        int globalIndex = 1;
        for (DemoTeamDto team : teams()) {
            String teamKey = team.getShortName();
            for (int i = 0; i < PLAYERS_PER_TEAM; i++) {
                int gi = globalIndex - 1;
                String name = NAME_GIVEN[gi % NAME_GIVEN.length]
                        + NAME_SUR[(gi / NAME_GIVEN.length) % NAME_SUR.length];
                list.add(player(globalIndex, teamKey, name, POSITIONS[i], JERSEYS[i], i == 0));
                globalIndex++;
            }
        }
        return list;
    }

    public static List<DemoMatchDto> matches() {
        List<DemoMatchDto> list = new ArrayList<>();
        int matchIndex = 1;
        for (String groupKey : GROUP_KEYS) {
            List<String> teamKeys = teamKeysInGroup(groupKey);
            for (int round = 1; round <= ROUND_ROBIN.length; round++) {
                int[][] pairs = ROUND_ROBIN[round - 1];
                for (int[] pair : pairs) {
                    String home = teamKeys.get(pair[0]);
                    String away = teamKeys.get(pair[1]);
                    boolean confirmed = round <= 2;
                    int homeScore = 0;
                    int awayScore = 0;
                    if (confirmed) {
                        int[] scores = generateScores(matchIndex, home, away);
                        homeScore = scores[0];
                        awayScore = scores[1];
                    }
                    int dayOffset = confirmed ? -(BasketLocalDemoConstants.CONFIRMED_MATCH_COUNT - matchIndex + 1)
                            : matchIndex - BasketLocalDemoConstants.CONFIRMED_MATCH_COUNT;
                    list.add(match(matchIndex, groupKey, home, away, round, homeScore, awayScore, dayOffset, confirmed));
                    matchIndex++;
                }
            }
        }
        return list;
    }

    private static DemoTeamDto team(String groupKey, String teamNo, String teamName, String englishName,
                                    String shortName, String description) {
        return DemoTeamDto.builder()
                .groupKey(groupKey)
                .teamNo(teamNo)
                .teamName(teamName)
                .englishName(englishName)
                .shortName(shortName)
                .description(description)
                .build();
    }

    private static DemoPlayerDto player(int index, String teamKey, String name, String position, String jersey,
                                        boolean captain) {
        return DemoPlayerDto.builder()
                .globalIndex(index)
                .teamKey(teamKey)
                .name(name)
                .positionCode(position)
                .jerseyNo(jersey)
                .captain(captain)
                .build();
    }

    private static DemoMatchDto match(int index, String groupKey, String home, String away, int round,
                                      int homeScore, int awayScore, int dayOffset, boolean confirmed) {
        return DemoMatchDto.builder()
                .matchIndex(index)
                .matchNo(String.format("LOCAL-DEMO-MATCH-%03d", index))
                .groupKey(groupKey)
                .homeTeamKey(home)
                .awayTeamKey(away)
                .roundNo(round)
                .homeScore(homeScore)
                .awayScore(awayScore)
                .dayOffset(dayOffset)
                .confirmed(confirmed)
                .build();
    }

    /** 按比赛序号稳定生成合理比分，主队略占优势但非绝对。 */
    private static int[] generateScores(int matchIndex, String home, String away) {
        long seed = 20260724L + matchIndex * 997L + home.hashCode() * 31L + away.hashCode();
        Random random = new Random(seed);
        int homeScore = 72 + random.nextInt(29);
        int awayScore = 68 + random.nextInt(29);
        if (matchIndex % 3 == 0 && awayScore <= homeScore) {
            awayScore = Math.min(99, homeScore + 1 + random.nextInt(8));
        } else if (awayScore >= homeScore) {
            awayScore = Math.max(65, homeScore - 1 - random.nextInt(10));
        }
        return new int[]{homeScore, awayScore};
    }

    public static int[] periodScores(int finalScore, long seed) {
        int[] periods = new int[4];
        int remaining = finalScore;
        Random random = new Random(seed);
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

    private static final int PLAYERS_PER_TEAM = BasketLocalDemoConstants.PLAYERS_PER_TEAM;
}
