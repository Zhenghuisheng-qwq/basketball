package com.zhs.response;

import io.swagger.annotations.ApiModel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.alibaba.fastjson2.JSONObject;

/**
 *
 * @author 
 * @since 2026-07-18
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketSiteMessageDetailResp")
public class BasketSiteMessageDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "接收球员编号")
        private Long receiverPlayerId;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "发送用户编号")
        private Long senderUserId;

        @ApiModelProperty(value = "消息类型", required = true)
        private String messageType;

        @ApiModelProperty(value = "消息标题", required = true)
        private String messageTitle;

        @ApiModelProperty(value = "消息内容")
        private String messageContent;

        @ApiModelProperty(value = "业务类型")
        private String businessType;

        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "业务编号")
        private Long businessId;

        @ApiModelProperty(value = "已读标志", required = true)
        private String readFlag;

        @ApiModelProperty(value = "已读时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date readTime;

        @ApiModelProperty(value = "发送时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date sendTime;

        @ApiModelProperty(value = "消息状态", required = true)
        private String messageStatus;

        @ApiModelProperty(value = "创建者")
        private String createBy;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
