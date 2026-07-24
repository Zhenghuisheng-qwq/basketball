package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("账户消息项")
public class MessageItemResp implements Serializable {

    @ApiModelProperty("接收记录主键")
    private String receiverId;

    @ApiModelProperty("消息类型")
    private String messageType;

    @ApiModelProperty("消息标题")
    private String title;

    @ApiModelProperty("消息内容")
    private String content;

    @ApiModelProperty("关联业务类型")
    private String bizType;

    @ApiModelProperty("关联业务主键")
    private String bizId;

    @ApiModelProperty("阅读状态：0 未读，1 已读")
    private Integer readStatus;

    @ApiModelProperty("发送时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sentTime;

    @ApiModelProperty("阅读时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date readTime;

    @ApiModelProperty("跳转类型")
    private String redirectType;

    @ApiModelProperty("跳转值")
    private String redirectValue;
}
