package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("安全操作日志项")
public class SecurityLogItemResp implements Serializable {

    @ApiModelProperty("日志主键")
    private String logId;

    @ApiModelProperty("安全事件类型")
    private String eventType;

    @ApiModelProperty("安全事件标题")
    private String eventTitle;

    @ApiModelProperty("变更前脱敏值")
    private String oldValueMasked;

    @ApiModelProperty("变更后脱敏值")
    private String newValueMasked;

    @ApiModelProperty("操作人类型")
    private String operatorType;

    @ApiModelProperty("脱敏客户端地址")
    private String clientIpMasked;

    @ApiModelProperty("终端类型")
    private String terminalType;

    @ApiModelProperty("事件时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date eventTime;

    @ApiModelProperty("备注")
    private String remark;
}
