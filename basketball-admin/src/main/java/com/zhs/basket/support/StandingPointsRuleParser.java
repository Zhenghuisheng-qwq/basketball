package com.zhs.basket.support;

import com.zhs.basket.domain.BasketTournamentRule;
import com.zhs.basket.enums.StandingTieBreakEnum;
import com.zhs.basket.enums.TournamentRuleTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class StandingPointsRuleParser {

    private StandingPointsRuleParser() {}

    public static StandingPointsRule requireFromRules(List<BasketTournamentRule> rules) {
        if (CollectionUtils.isEmpty(rules)) {
            throw new ServiceException("积分规则未配置");
        }
        BasketTournamentRule rule = rules.stream()
                .filter(r -> r != null && TournamentRuleTypeEnum.isStandingPoints(r.getRuleType()))
                .filter(r -> r.getRuleStatus() == null || r.getRuleStatus() == 1)
                .filter(r -> r.getIsDelete() == null || r.getIsDelete() == 0)
                .max(Comparator.comparing(StandingPointsRuleParser::versionOrder)
                        .thenComparing(r -> r.getEffectiveTime() == null ? 0L : r.getEffectiveTime().getTime())
                        .thenComparing(r -> r.getId() == null ? 0L : r.getId()))
                .orElseThrow(() -> new ServiceException("积分规则未配置"));
        return parse(rule.getRuleContent());
    }

    public static StandingPointsRule parse(String content) {
        if (!StringUtils.hasText(content)) {
            throw new ServiceException("积分规则未配置");
        }
        StandingPointsRule rule = new StandingPointsRule();
        for (String rawLine : content.split("\\R")) {
            String line = rawLine == null ? "" : rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim().toUpperCase();
            String value = line.substring(eq + 1).trim();
            switch (key) {
                case "WIN_POINTS" -> rule.setWinPoints(parseDecimal(value, key));
                case "LOSS_POINTS" -> rule.setLossPoints(parseDecimal(value, key));
                case "FORFEIT_WIN_POINTS" -> rule.setForfeitWinPoints(parseDecimal(value, key));
                case "FORFEIT_LOSS_POINTS" -> rule.setForfeitLossPoints(parseDecimal(value, key));
                case "TIE_BREAK" -> rule.setTieBreakOrder(parseTieBreak(value));
                default -> {
                    // 忽略未知键，避免规则扩展时阻断
                }
            }
        }
        if (rule.getWinPoints() == null || rule.getLossPoints() == null) {
            throw new ServiceException("积分规则未配置：缺少 WIN_POINTS 或 LOSS_POINTS");
        }
        if (rule.getForfeitWinPoints() == null) {
            rule.setForfeitWinPoints(rule.getWinPoints());
        }
        if (rule.getForfeitLossPoints() == null) {
            rule.setForfeitLossPoints(rule.getLossPoints());
        }
        if (rule.getTieBreakOrder() == null || rule.getTieBreakOrder().isEmpty()) {
            List<StandingTieBreakEnum> defaults = new ArrayList<>();
            defaults.add(StandingTieBreakEnum.STANDING_POINTS);
            defaults.add(StandingTieBreakEnum.POINT_DIFF);
            defaults.add(StandingTieBreakEnum.POINTS_FOR);
            defaults.add(StandingTieBreakEnum.WINS);
            defaults.add(StandingTieBreakEnum.TEAM_ID);
            rule.setTieBreakOrder(defaults);
        }
        if (!rule.getTieBreakOrder().contains(StandingTieBreakEnum.TEAM_ID)) {
            rule.getTieBreakOrder().add(StandingTieBreakEnum.TEAM_ID);
        }
        return rule;
    }

    private static List<StandingTieBreakEnum> parseTieBreak(String value) {
        List<StandingTieBreakEnum> list = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            StandingTieBreakEnum item = StandingTieBreakEnum.fromCode(part.trim());
            if (!list.contains(item)) {
                list.add(item);
            }
        }
        return list;
    }

    private static BigDecimal parseDecimal(String value, String key) {
        try {
            return new BigDecimal(value.trim());
        } catch (Exception ex) {
            throw new ServiceException("积分规则配置非法: " + key);
        }
    }

    private static long versionOrder(BasketTournamentRule rule) {
        if (rule == null || !StringUtils.hasText(rule.getVersionNo())) {
            return 0L;
        }
        String digits = rule.getVersionNo().replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            return 0L;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }
}
