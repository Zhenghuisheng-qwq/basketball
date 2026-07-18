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
 * 球队信息�?
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketTeam implements Serializable {

    //===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "球队编号")
    private String teamNo;

    @ApiModelProperty(value = "球队名称")
    private String teamName;

    @ApiModelProperty(value = "球队标志")
    private String logoUrl;

    @ApiModelProperty(value = "比赛终端")
    private String terminalType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "队长球员编号")
    private Long captainPlayerId;


    @ApiModelProperty(value = "所在城")
    private String city;

    @ApiModelProperty(value = "球队口号")
    private String slogan;

    @ApiModelProperty(value = "球队介绍")
    private String teamDesc;

    @ApiModelProperty(value = "成员数量")
    private Integer memberCount;

    @ApiModelProperty(value = "锁定标志")
    private String lockedFlag;

    @ApiModelProperty(value = "球队状态")
    private String teamStatus;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新者")
    private String updateBy;

    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "备注")
    private String remark;



    @ApiModelProperty(value = "逻辑删除：0未删 1已删")
    private Integer isDelete;

//===========================自定义字段=================================

}
