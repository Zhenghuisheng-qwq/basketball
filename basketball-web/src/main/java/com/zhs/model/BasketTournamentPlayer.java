package com.zhs.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;
import org.springframework.format.annotation.DateTimeFormat;
import java.io.Serializable;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.alibaba.fastjson2.JSONObject;


/**
 * 赛事球员快照�?
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketTournamentPlayer implements Serializable {

    //===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事编号")
    private Long tournamentId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "赛事球队编号")
    private Long tournamentTeamId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "来源球员编号")
    private Long sourcePlayerId;


    @ApiModelProperty(value = "球员编号")
    private String playerNo;

    @ApiModelProperty(value = "球员名称")
    private String playerName;

    @ApiModelProperty(value = "头像地址")
    private String avatarUrl;

    @ApiModelProperty(value = "游戏账号")
    private String gameAccount;

    @ApiModelProperty(value = "游戏昵称")
    private String gameRoleName;

    @ApiModelProperty(value = "球衣号码")
    private Integer jerseyNo;

    @ApiModelProperty(value = "场上位置")
    private String positionName;

    @ApiModelProperty(value = "首发标志")
    private String starterFlag;

    @ApiModelProperty(value = "队长标志")
    private String captainFlag;

    @ApiModelProperty(value = "球员状态")
    private String playerStatus;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;




    @ApiModelProperty(value = "逻辑删除：0未删 1已删")
    private Integer isDelete;

//===========================自定义字段=================================

}
