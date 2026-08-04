package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.List;

@Data
public class BasketDivisionStageVo {
    private Long id;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private String stageTypeName;
    private String competitionFormat;
    private String competitionFormatName;
    private String groupingMode;
    private String groupingModeName;
    private Integer groupCount;
    private Integer roundRobinCycles;
    private Integer bestOf;
    private Integer winPoints;
    private Integer lossPoints;
    private Integer forfeitWinPoints;
    private Integer forfeitLossPoints;
    private String advancementType;
    private String advancementTypeName;
    private Integer advancementCount;
    private String pairingRule;
    private String pairingRuleName;
    private String nextStageCode;
    private Integer requiresManualConfirmation;
    private String stageStatus;
    private List<BasketDivisionTieBreakRuleVo> tieBreakRules;
}
