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

/**
 * 球员平台游戏 ID 变更申请表
 */
@Data
@ApiModel
public class BasketPlayerPlatformChangeRequest implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;

    @ApiModelProperty(value = "申请编号")
    private String requestNo;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "球员主键")
    private Long playerId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "会员主键")
    private Long memberId;

    @ApiModelProperty(value = "变更类型")
    private String changeType;

    @ApiModelProperty(value = "游戏平台")
    private String platform;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "原平台账号主键")
    private Long platformAccountId;

    @ApiModelProperty(value = "变更前展示游戏 ID")
    private String oldDisplayGameId;

    @ApiModelProperty(value = "变更后展示游戏 ID")
    private String newDisplayGameId;

    @ApiModelProperty(value = "变更后规范化游戏 ID")
    private String newNormalizedGameId;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "证明材料文件主键")
    private Long proofFileId;

    @ApiModelProperty(value = "申请状态")
    private Integer requestStatus;

    @ApiModelProperty(value = "提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @ApiModelProperty(value = "审核完成时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "审核人主键")
    private Long reviewerId;

    @ApiModelProperty(value = "审核说明")
    private String reviewRemark;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "生效后平台账号主键")
    private Long resultPlatformAccountId;

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
}
