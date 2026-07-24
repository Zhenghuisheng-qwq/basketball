package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("邮箱验证码登录请求")
public class MemberCodeLoginReq implements Serializable {

    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "验证码业务流水号", required = true)
    private String verifyBizNo;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String verifyCode;

    @ApiModelProperty(value = "终端类型")
    private String terminalType;
}
