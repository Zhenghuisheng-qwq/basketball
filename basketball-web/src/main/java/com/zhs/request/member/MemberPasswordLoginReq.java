package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("密码登录请求")
public class MemberPasswordLoginReq implements Serializable {

    @ApiModelProperty(value = "账号：邮箱 / 手机号 / 账号编号", required = true)
    private String account;

    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @ApiModelProperty(value = "终端类型")
    private String terminalType;
}
