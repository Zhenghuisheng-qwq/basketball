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
    private String organizationMode;
    private String competitionFormat;
    private String groupingMode;
    private Integer groupCount;
    private Integer roundRobinCycles;
    private Integer bestOf;
    private Integer semifinalBestOf;
    private Integer finalBestOf;
    private Integer winPoints;
    private Integer lossPoints;
    private Integer forfeitWinPoints;
    private Integer forfeitLossPoints;
    private String advancementType;
    private Integer advancementCount;
    private String pairingRule;
    private String nextStageCode;
    private String seedRule;
    private String drawRule;
    private String byeRule;
    private Integer requiresManualConfirmation;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
