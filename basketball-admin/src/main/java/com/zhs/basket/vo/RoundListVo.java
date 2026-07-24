package com.zhs.basket.vo;

import java.util.Date;
import lombok.Data;

@Data
public class RoundListVo {
    private Long id;
    private Long seasonId;
    private Long stageId;
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
}
