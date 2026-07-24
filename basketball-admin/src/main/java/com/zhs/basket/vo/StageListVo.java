package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class StageListVo {
    private Long id;
    private Long seasonId;
    private String seasonName;
    private String tournamentName;
    private String stageNo;
    private String stageName;
    private String stageType;
    private String formatType;
    private Date startTime;
    private Date endTime;
    private Integer sequenceNo;
    private Integer stageStatus;
    private String stageStatusName;
    private Integer publicStatus;
    private Date createTime;
}
