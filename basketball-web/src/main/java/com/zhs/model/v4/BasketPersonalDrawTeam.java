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
@TableName("basket_personal_draw_team")
public class BasketPersonalDrawTeam implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long drawBatchId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    private String temporaryTeamCode;
    private String temporaryTeamName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainPersonalRegistrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainUserId;

    private Integer expectedMemberCount;
    private Integer actualMemberCount;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long longTermTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long convertedRegistrationId;

    private String formationStatus;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long confirmedBy;

    private Date confirmedTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
