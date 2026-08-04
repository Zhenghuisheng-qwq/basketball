package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketFormatTemplateStage {
    private Long id;
    private Long templateId;
    private String stageCode;
    private String stageName;
    private Integer stageOrder;
    private String stageType;
    private String competitionFormat;
    private String groupingMode;
    private Integer groupCount;
    private Integer roundRobinCycles;
    private Integer bestOf;
    private Integer winPoints;
    private Integer lossPoints;
    private Integer forfeitWinPoints;
    private Integer forfeitLossPoints;
    private String advancementType;
    private Integer advancementCount;
    private String pairingRule;
    private String nextStageCode;
    private Integer requiresManualConfirmation;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
