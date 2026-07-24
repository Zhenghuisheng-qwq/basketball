package com.zhs.basket.domain;

import lombok.Data;
import java.util.Date;

@Data
public class BasketMatchStatusLog {
    private Long id;
    private Long matchId;
    private Integer beforeStatus;
    private Integer afterStatus;
    private String changeType;
    private String changeReason;
    private Long operatorId;
    private String operatorName;
    private Date changeTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
