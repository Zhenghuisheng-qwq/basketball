package com.zhs.response.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("阵容变更申请列表项")
public class RosterChangeRequestItemResp implements Serializable {

    private String requestId;
    private String requestNo;
    private String seasonId;
    private String changeType;
    private String changeTypeText;
    private Integer requestStatus;
    private String requestStatusText;
    private String playerId;
    private String sourceEventTeamId;
    private String targetEventTeamId;
    private String applyReason;
    private String rejectReason;
    private Boolean canWithdraw;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;
}
