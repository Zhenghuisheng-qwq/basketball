package com.zhs.response.account;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("登录日志项")
public class LoginLogItemResp implements Serializable {

    @ApiModelProperty("日志主键")
    private String logId;

    @ApiModelProperty("登录方式")
    private String loginType;

    @ApiModelProperty("登录结果")
    private Integer loginResult;

    @ApiModelProperty("失败原因")
    private String failureReason;

    @ApiModelProperty("脱敏客户端地址")
    private String clientIpMasked;

    @ApiModelProperty("客户端地区")
    private String clientRegion;

    @ApiModelProperty("终端类型")
    private String terminalType;

    @ApiModelProperty("脱敏客户端标识")
    private String userAgentMasked;

    @ApiModelProperty("登录时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date loginTime;
}
