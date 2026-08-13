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
 * CNPA 赛事域 V4 实体：BasketEventTeam
 */
@Data
@ApiModel
@TableName("basket_event_team")
public class BasketEventTeam implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long rosterVersionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long registrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourceTeamId;

    private String teamNameSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamLogoFileId;

    private String teamShortName;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long captainMemberId;

    private String eventTeamNo;

    private String eventTeamStatus;

    private String platformCode;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
