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
@ApiModel("比赛数据提交主表")
public class BasketMatchDataSubmission implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private String submissionNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long roundId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long groupId;

    private String platformCode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long homeTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long awayTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitterTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitterMemberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitterPlayerId;

    private String submitterRole;
    private Integer versionNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long previousSubmissionId;

    private Integer homeScore;
    private Integer awayScore;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long winnerTeamId;

    private Integer submissionStatus;
    private String auditStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submittedTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long reviewedBy;

    private String reviewedByName;
    private String rejectReason;
    private String auditRemark;
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
