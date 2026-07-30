package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("修改登录邮箱请求")
public class AccountChangeEmailReq implements Serializable {

    @ApiModelProperty(value = "新邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "验证码业务流水号", required = true)
    private String verifyBizNo;

    @ApiModelProperty(value = "邮箱验证码", required = true)
    private String verifyCode;
}
