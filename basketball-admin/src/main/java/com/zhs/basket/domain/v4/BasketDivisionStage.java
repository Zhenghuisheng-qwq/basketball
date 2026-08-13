package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketDivisionStage {
    private Long id;
    private Long snapshotId;
    private Long divisionId;
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
    /** Knockout semifinal BO; null means inherit bestOf (legacy). */
    private Integer semifinalBestOf;
    /** Knockout final BO; null means inherit bestOf (legacy). */
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
    private String stageStatus;
    private String executionStatus;
    private Long scheduleBatchId;
    private Integer participantCount;
    private Integer qualifiedCount;
    private Date startedTime;
    private Date completedTime;
    private Date confirmedTime;
    private Long confirmedBy;
    private String currentUniqueKey;
    private Integer executionVersion;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
