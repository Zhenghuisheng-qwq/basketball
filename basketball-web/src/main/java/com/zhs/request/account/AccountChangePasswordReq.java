package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("修改密码请求")
public class AccountChangePasswordReq implements Serializable {

    @ApiModelProperty(value = "当前密码", required = true)
    private String oldPassword;

    @ApiModelProperty(value = "新密码", required = true)
    private String newPassword;

    @ApiModelProperty(value = "确认新密码", required = true)
    private String confirmPassword;
}
