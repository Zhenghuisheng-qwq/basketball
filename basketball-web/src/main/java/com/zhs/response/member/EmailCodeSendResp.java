package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("发送邮箱验证码响应")
public class EmailCodeSendResp implements Serializable {

    @ApiModelProperty("业务流水号")
    private String bizNo;

    @ApiModelProperty("有效秒数")
    private Long expireSeconds;

    @ApiModelProperty("重发间隔秒数")
    private Long resendSeconds;

    @ApiModelProperty("是否为开发模式（任意6位数字验证码可通过，上线前请关闭）")
    private Boolean devBypass;
}
