package com.zhs.basket.support;

import com.zhs.basket.enums.StandingTieBreakEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 从 basket_tournament_rule(rule_type=STANDING_POINTS) 的 rule_content 解析出的积分规则。
 * <p>
 * 约定格式（每行 KEY=VALUE，# 注释）：
 * <pre>
 * WIN_POINTS=2
 * LOSS_POINTS=1
 * FORFEIT_WIN_POINTS=2
 * FORFEIT_LOSS_POINTS=0
 * TIE_BREAK=STANDING_POINTS,HEAD_TO_HEAD_POINTS,HEAD_TO_HEAD_DIFF,POINT_DIFF,POINTS_FOR,WINS,TEAM_ID
 * </pre>
 */
@Data
public class StandingPointsRule {

    private BigDecimal winPoints;
    private BigDecimal lossPoints;
    private BigDecimal forfeitWinPoints;
    private BigDecimal forfeitLossPoints;
    private List<StandingTieBreakEnum> tieBreakOrder = new ArrayList<>();

    public List<StandingTieBreakEnum> getTieBreakOrder() {
        return tieBreakOrder == null ? Collections.emptyList() : tieBreakOrder;
    }
}
