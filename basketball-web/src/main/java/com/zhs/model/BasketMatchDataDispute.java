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
@ApiModel("比赛数据争议")
@TableName("basket_match_data_dispute")
public class BasketMatchDataDispute implements Serializable {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String disputeNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submissionId;

    private String initiatorType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long initiatorId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long initiatorMemberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long initiatorPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long initiatorTeamId;

    private String disputeType;
    private String originalValue;
    private String expectedValue;
    private String disputeReason;
    private String evidenceFileIds;
    private String disputeStatus;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date disputeDeadlineTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long processedBy;

    private String processedByName;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date processedTime;

    private String processResult;
    private String rejectReason;
    private String correctionType;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long handlerId;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date handleTime;

    private String handleResult;
    private String handleRemark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long revisionId;

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
