package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("批量初始化账号条目")
public class MemberInitAccountItemResp implements Serializable {

    @ApiModelProperty("邮箱")
    private String email;

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员认证申请主键")
    private String applicationId;

    @ApiModelProperty("球员认证申请状态")
    private Integer applicationStatus;
}
