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
@TableName("basket_personal_registration")
public class BasketPersonalRegistration implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seriesId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerPlatformId;

    private String actualPlatformCode;
    private String gameAccountId;
    private String preferredPosition;
    private String registrationStatus;
    private Date submitTime;
    private Date approvedTime;
    private String rejectReason;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long assignedDrawTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long convertedRegistrationId;

    private String activeUniqueKey;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;

    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
