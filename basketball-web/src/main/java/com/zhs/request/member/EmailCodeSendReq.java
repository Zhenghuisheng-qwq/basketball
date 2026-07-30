package com.zhs.request.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("发送邮箱验证码请求")
public class EmailCodeSendReq implements Serializable {

    @ApiModelProperty(value = "邮箱", required = true)
    private String email;

    @ApiModelProperty(value = "用途：REGISTER / RESET_PASSWORD / CHANGE_EMAIL", required = true)
    private String purpose;
}
