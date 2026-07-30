package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("密码登录请求")
public class MemberPasswordLoginReq implements Serializable {

    @ApiModelProperty(value = "注册邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "密码", required = true)
    private String password;

    @ApiModelProperty(value = "终端类型")
    private String terminalType;
}
