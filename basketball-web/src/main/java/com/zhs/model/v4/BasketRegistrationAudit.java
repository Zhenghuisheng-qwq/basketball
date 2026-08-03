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
 * CNPA 赛事域 V4 实体：BasketRegistrationAudit
 */
@Data
@ApiModel
@TableName("basket_registration_audit")
public class BasketRegistrationAudit implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long registrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    private String auditAction;

    private String auditResult;

    private String beforeStatus;

    private String afterStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long auditUserId;

    private String auditUserName;

    private Date auditTime;

    private String auditRemark;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
