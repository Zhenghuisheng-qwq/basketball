package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("用户登录响应")
public class MemberLoginResp implements Serializable {

    @ApiModelProperty("UUID Token")
    private String token;

    @ApiModelProperty("Token 类型")
    private String tokenType;

    @ApiModelProperty("过期秒数")
    private Long expireSeconds;

    @ApiModelProperty("用户摘要")
    private MemberSummaryResp member;
}
