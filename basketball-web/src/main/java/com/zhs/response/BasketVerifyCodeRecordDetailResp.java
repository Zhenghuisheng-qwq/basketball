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
 * @since 2026-07-24
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@ApiModel(value = "BasketVerifyCodeRecordDetailResp")
public class BasketVerifyCodeRecordDetailResp implements Serializable {
        //表字段
        @JsonSerialize(using = ToStringSerializer.class)
        @ApiModelProperty(value = "主键")
        private Long id;

        @ApiModelProperty(value = "业务编号", required = true)
        private String bizNo;

        @ApiModelProperty(value = "发送渠道", required = true)
        private String channelType;

        @ApiModelProperty(value = "接收地址", required = true)
        private String receiver;

        @ApiModelProperty(value = "验证码用途", required = true)
        private String purposeType;

        @ApiModelProperty(value = "验证码摘要", required = true)
        private String codeHash;

        @ApiModelProperty(value = "发送状态", required = true)
        private Integer sendStatus;

        @ApiModelProperty(value = "验证状态", required = true)
        private Integer verifyStatus;

        @ApiModelProperty(value = "过期时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date expireTime;

        @ApiModelProperty(value = "验证时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date verifyTime;

        @ApiModelProperty(value = "发送服务商")
        private String sendProvider;

        @ApiModelProperty(value = "服务商消息编号")
        private String providerMessageId;

        @ApiModelProperty(value = "客户端地址")
        private String clientIp;

        @ApiModelProperty(value = "失败原因")
        private String failureReason;

        @ApiModelProperty(value = "创建时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date createTime;

        @ApiModelProperty(value = "更新时间")
        @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private Date updateTime;

        @ApiModelProperty(value = "删除标记", required = true)
        private Integer isDelete;

        //=====================自定义字段=====================

        //id
        private List<String> idList;
}
