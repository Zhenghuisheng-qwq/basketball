package com.zhs.basket.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class CaptainGrantLogVo {
    private Long id;
    private Long grantId;
    private Long memberId;
    private Long playerId;
    private Long teamId;
    private String teamName;
    private Long seasonId;
    private String seasonName;
    private String tournamentName;
    private String eventName;
    private String actionType;
    private String actionTypeText;
    private Integer beforeStatus;
    private String beforeStatusText;
    private Integer afterStatus;
    private String afterStatusText;
    private Integer paymentConfirmed;
    private String paymentReference;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date grantTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expireTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date revokeTime;
    private Long operatorId;
    private String operatorName;
    private String operatorType;
    private String reason;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
