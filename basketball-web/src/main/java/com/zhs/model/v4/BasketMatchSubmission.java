package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSubmission
 */
@Data
@ApiModel
@TableName("basket_match_submission")
public class BasketMatchSubmission implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long opponentEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitEventPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitMemberId;

    private String submitterRole;

    private String submissionStatus;

    private Integer versionNo;

    private Integer homeScore;

    private Integer awayScore;

    private Date submitTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditUserId;

    private String auditUserName;

    private Date auditTime;

    private String auditRemark;

    private String rejectReason;

    private String submitRemark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesSubmissionId;

    private String submissionMode;

    private Integer draftComplete;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
