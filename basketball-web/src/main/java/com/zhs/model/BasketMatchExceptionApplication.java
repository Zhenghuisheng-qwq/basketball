package com.zhs.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("比赛异常申请")
@TableName("basket_match_exception_application")
public class BasketMatchExceptionApplication implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String applicationNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    private String platformCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantMemberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long opponentTeamId;

    private String exceptionType;
    private String exceptionReason;
    private String applicationStatus;
    private String decisionType;
    private String decisionResult;
    private Integer decisionScoreHome;
    private Integer decisionScoreAway;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long walkoverTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long processedBy;

    private String processedByName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processedTime;

    private String remark;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    private Integer isDelete;
}
