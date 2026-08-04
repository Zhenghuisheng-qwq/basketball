package com.zhs.model.v4;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@TableName("basket_personal_registration_audit")
public class BasketPersonalRegistrationAudit implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long personalRegistrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String beforeStatus;
    private String afterStatus;
    private String auditAction;
    private String auditRemark;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long operatorId;

    private Date operateTime;
    private Date createTime;
    private Integer isDelete;
}
