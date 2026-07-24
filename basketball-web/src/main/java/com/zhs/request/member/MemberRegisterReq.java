package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("官网会员注册请求")
public class MemberRegisterReq implements Serializable {

    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @ApiModelProperty(value = "确认密码", required = true)
    private String confirmPassword;

    @ApiModelProperty(value = "昵称", required = true)
    private String nickname;

    @ApiModelProperty(value = "手机号（表字段非空）", required = true)
    private String mobile;

    @ApiModelProperty(value = "手机区号，默认 +86")
    private String mobileCountryCode;

    @ApiModelProperty(value = "终端类型")
    private String terminalType;

    @ApiModelProperty(value = "验证码业务流水号", required = true)
    private String verifyBizNo;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String verifyCode;

    @ApiModelProperty(value = "同意用户协议", required = true)
    private Boolean agreeUserAgreement;

    @ApiModelProperty(value = "同意隐私政策", required = true)
    private Boolean agreePrivacyPolicy;
}
