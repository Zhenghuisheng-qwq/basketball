package com.zhs.basket.vo.v4;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class BasketStandingPointAdjustmentVo {
    private Long id;
    private Long divisionId;
    private Long stageId;
    private Long eventTeamId;
    private Integer delta;
    private String reason;
    private String remark;
    private Long operatorId;
    private String operatorName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
