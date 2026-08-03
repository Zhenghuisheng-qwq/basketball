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
 * CNPA 赛事域 V4 实体：BasketEventPlayer
 */
@Data
@ApiModel
@TableName("basket_event_player")
public class BasketEventPlayer implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long eventTeamId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sourcePlayerId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long memberId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long platformIdentityId;

    private String platformCode;

    private String gameIdSnapshot;

    private String playerNameSnapshot;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long avatarFileId;

    private String position;

    private String jerseyNo;

    private Integer isCaptain;

    private Integer isStarter;

    private String eventPlayerStatus;

    private Date createTime;

    private Date updateTime;

    private Integer isDelete;

}
