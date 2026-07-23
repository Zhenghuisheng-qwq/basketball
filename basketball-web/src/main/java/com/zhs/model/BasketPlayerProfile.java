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
 * 球员档案表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketPlayerProfile implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "会员主键")
    private Long memberId;


    @ApiModelProperty(value = "永久球员编号")
    private String playerNo;

    @ApiModelProperty(value = "认证姓名")
    private String realName;

    @ApiModelProperty(value = "证件类型")
    private String idType;

    @ApiModelProperty(value = "证件号码密文")
    private String idNoCipher;

    @ApiModelProperty(value = "证件号码脱敏值")
    private String idNoMasked;

    @ApiModelProperty(value = "比赛终端")
    private String gameTerminal;

    @ApiModelProperty(value = "游戏账号")
    private String gameAccount;

    @ApiModelProperty(value = "游戏昵称")
    private String gameNickname;

    @ApiModelProperty(value = "场上位置")
    private String positionCode;

    @ApiModelProperty(value = "惯用手")
    private String dominantHand;

    @ApiModelProperty(value = "身高")
    private java.math.BigDecimal heightCm;

    @ApiModelProperty(value = "体重")
    private java.math.BigDecimal weightKg;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "当前球队主键")
    private Long currentTeamId;


    @ApiModelProperty(value = "认证状态")
    private Integer certificationStatus;

    @ApiModelProperty(value = "球员状态")
    private Integer playerStatus;

    @ApiModelProperty(value = "认证通过时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date certifiedTime;


    @ApiModelProperty(value = "档案公开状态")
    private Integer profileVisible;

    @ApiModelProperty(value = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;


    @ApiModelProperty(value = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @ApiModelProperty(value = "删除标记")
    private Integer isDelete;


//===========================自定义字段=================================

}
