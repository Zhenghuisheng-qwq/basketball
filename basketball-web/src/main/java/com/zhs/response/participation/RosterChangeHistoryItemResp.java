package com.zhs.response.participation;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("阵容变更历史项")
public class RosterChangeHistoryItemResp implements Serializable {

    private String recordId;
    private String changeType;
    private String changeTypeText;
    private String playerId;
    private String sourceEventTeamId;
    private String targetEventTeamId;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String operateReason;
    private String operatorName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;
}
