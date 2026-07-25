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

    @ApiModelProperty("球员参赛资格状态：1 正常 / 2 受限")
    private Integer playerStatus;

    @ApiModelProperty("队长资格状态（无资格时为 null）")
    private Integer captainQualificationStatus;
}
