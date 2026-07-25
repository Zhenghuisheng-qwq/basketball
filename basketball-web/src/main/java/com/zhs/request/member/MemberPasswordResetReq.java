package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("找回密码请求")
public class MemberPasswordResetReq implements Serializable {

    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "验证码流水号", required = true)
    private String verifyBizNo;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String verifyCode;

    @ApiModelProperty(value = "新密码", required = true)
    private String password;

    @ApiModelProperty(value = "确认新密码", required = true)
    private String confirmPassword;
}
