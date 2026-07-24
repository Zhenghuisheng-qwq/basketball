package com.zhs.basket.req;

import java.util.Date;
import lombok.Data;

@Data
public class StageUpdateReq {
    private String stageName;
    private String stageType;
    private String formatType;
    private Date startTime;
    private Date endTime;
    private Integer sequenceNo;
    private String qualificationRuleText;
    private Integer publicStatus;
}
