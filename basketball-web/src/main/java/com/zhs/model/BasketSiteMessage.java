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
 * 站内消息�?
 *
 * @author 
 * @since 2026-07-18
 */

@Data
@ApiModel
public class BasketSiteMessage implements Serializable {

    //===========================数据库字段================================

    @JsonSerialize(using = ToStringSerializer.class)
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private Long id;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "接收球员编号")
    private Long receiverPlayerId;


    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "发送用户编")
    private Long senderUserId;


    @ApiModelProperty(value = "消息类型")
    private String messageType;

    @ApiModelProperty(value = "消息标题")
    private String messageTitle;

    @ApiModelProperty(value = "消息内容")
    private String messageContent;

    @ApiModelProperty(value = "业务类型")
    private String businessType;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "业务编号")
    private Long businessId;


    @ApiModelProperty(value = "已读标志")
    private String readFlag;

    @ApiModelProperty(value = "已读时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;


    @ApiModelProperty(value = "发送时")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;


    @ApiModelProperty(value = "消息状态")
    private String messageStatus;

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
