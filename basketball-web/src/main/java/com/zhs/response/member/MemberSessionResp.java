package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("当前会话会员信息")
public class MemberSessionResp extends MemberSummaryResp {

    @ApiModelProperty("注册来源")
    private String registerSource;

    @ApiModelProperty("注册终端")
    private String registerTerminal;
}
