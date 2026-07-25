package com.zhs.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel
public class BasketTournamentRosterChangeRequest implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String requestNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String changeType;
    private Integer requestStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long applicantMemberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long targetEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceRosterId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long newCaptainPlayerId;

    private String targetRosterRole;
    private Integer targetIsStarting;
    private Integer targetIsSubstitute;
    private String applyReason;
    private String rejectReason;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewerId;

    private String reviewRemark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long resultRosterId;

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
