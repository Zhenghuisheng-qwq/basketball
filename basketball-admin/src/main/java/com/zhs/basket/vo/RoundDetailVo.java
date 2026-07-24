package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class RoundDetailVo {
    private Long id;
    private Long seasonId;
    private Long stageId;
    private String stageName;
    private String roundNo;
    private String roundName;
    private String roundType;
    private Integer sequenceNo;
    private Integer bestOf;
    private Date startTime;
    private Date endTime;
    private Integer roundStatus;
    private String roundStatusName;
    private Date createTime;
    private Date updateTime;
    private Boolean canEdit;
    private Boolean canActivate;
    private Boolean canFinish;
    private Boolean canCancel;
    private Boolean canDelete;
}
