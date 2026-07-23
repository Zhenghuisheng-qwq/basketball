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
 * 消息任务表
 *
 * @author 
 * @since 2026-07-24
 */

@Data
@ApiModel
public class BasketMessageTask implements Serializable {
//===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @ApiModelProperty(value = "任务编号")
    private String taskNo;

    @ApiModelProperty(value = "消息类型")
    private String messageType;

    @ApiModelProperty(value = "消息标题")
    private String title;

    @ApiModelProperty(value = "消息内容")
    private Object contentText;

    @ApiModelProperty(value = "关联业务类型")
    private String bizType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "关联业务主键")
    private Long bizId;


    @ApiModelProperty(value = "发送范围")
    private String sendScope;

    @ApiModelProperty(value = "发送状态")
    private Integer sendStatus;

    @ApiModelProperty(value = "计划发送时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date scheduledTime;


    @ApiModelProperty(value = "实际发送时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sentTime;


    @ApiModelProperty(value = "接收总数")
    private Integer totalCount;

    @ApiModelProperty(value = "成功数量")
    private Integer successCount;

    @ApiModelProperty(value = "失败数量")
    private Integer failCount;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "创建人主键")
    private Long creatorId;


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
