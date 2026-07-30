package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("官网用户注册响应")
public class MemberRegisterResp implements Serializable {

    @ApiModelProperty("注册邮箱（已标准化）")
    private String email;
}
