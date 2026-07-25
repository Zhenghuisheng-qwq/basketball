package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("修改密码响应")
public class AccountChangePasswordResp {

    @ApiModelProperty("是否需要重新登录")
    private Boolean needRelogin = true;
}
