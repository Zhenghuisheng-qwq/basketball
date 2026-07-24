package com.zhs.basket.vo;

import lombok.Data;
import java.util.Date;

@Data
public class MatchStatusLogVo {
    private Long id;
    private Integer beforeStatus;
    private String beforeStatusName;
    private Integer afterStatus;
    private String afterStatusName;
    private String changeType;
    private String changeTypeName;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private Date changeTime;
}
