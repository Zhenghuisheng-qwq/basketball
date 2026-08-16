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

@Data
@ApiModel
@TableName("basket_match_series_submission")
public class BasketMatchSeriesSubmission implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long matchSeriesId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long stageId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitEventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long submitMemberId;

    private Integer declaredHomeWinCount;

    private Integer declaredAwayWinCount;

    private Integer actualGameCount;

    private String status;

    private Integer versionNo;

    private Date submitTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditUserId;

    private String auditUserName;

    private Date auditTime;

    private String auditRemark;

    private String rejectReason;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;
}
