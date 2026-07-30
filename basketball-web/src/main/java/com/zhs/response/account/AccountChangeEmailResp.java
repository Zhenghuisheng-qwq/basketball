package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("修改登录邮箱响应")
public class AccountChangeEmailResp implements Serializable {

    @ApiModelProperty("是否需要重新登录")
    private Boolean needRelogin;
}
