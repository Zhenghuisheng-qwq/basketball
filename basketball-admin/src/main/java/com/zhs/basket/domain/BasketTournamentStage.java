package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketTournamentStage {
    private Long id;
    private Long seasonId;
    private String stageNo;
    private String stageName;
    private String stageType;
    private String formatType;
    private Date startTime;
    private Date endTime;
    private Integer sequenceNo;
    private Integer stageStatus;
    private String qualificationRuleText;
    private Integer publicStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
