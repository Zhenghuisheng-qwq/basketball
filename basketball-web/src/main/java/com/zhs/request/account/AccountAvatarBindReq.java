package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("账户头像绑定请求")
public class AccountAvatarBindReq implements Serializable {

    @ApiModelProperty(value = "头像文件主键", required = true)
    private String fileId;
}
