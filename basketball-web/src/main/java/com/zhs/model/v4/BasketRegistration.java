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
 * CNPA 赛事域 V4 实体：BasketRegistration
 */
@Data
@ApiModel
@TableName("basket_registration")
public class BasketRegistration implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String registrationMode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainMemberId;

    private String registrationStatus;

    private Integer lineupCount;

    private Date submitTime;

    private Date approveTime;

    private String rejectReason;

    private Date withdrawTime;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditUserId;

    private String auditRemark;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
