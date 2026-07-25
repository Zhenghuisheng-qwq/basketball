package com.zhs.model;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel
public class BasketTournamentSeasonRoster implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @ApiModelProperty(value = "赛事阵容编号")
    private String rosterNo;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long teamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long registrationId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long registrationPlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long openRegistrationId;

    private String playerNicknameSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long avatarFileIdSnapshot;

    private String platformSnapshot;
    private String gameIdDisplaySnapshot;
    private String gameIdNormalizedSnapshot;
    private String positionSnapshot;
    private String rosterRole;
    private Integer isCaptain;
    private Integer isStarting;
    private Integer isSubstitute;
    private String sourceType;
    private Integer rosterStatus;
    private Integer lockStatus;
    private String activeSlot;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date invalidTime;

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
