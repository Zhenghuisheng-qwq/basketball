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
 * CNPA 赛事域 V4 实体：BasketCaptainGrant
 */
@Data
@ApiModel
@TableName("basket_captain_grant")
public class BasketCaptainGrant implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;

    private String grantStatus;

    /** ACTIVE 时写入 divisionId_teamId；REVOKED/EXPIRED/软删除为 null */
    private String activeUniqueKey;

    private Date validFrom;

    private Date validTo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long grantedBy;

    private String remark;

    private String grantSource;

    private Long sourceDrawTeamId;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
