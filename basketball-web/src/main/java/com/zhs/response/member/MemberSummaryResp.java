package com.zhs.response.member;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("会员摘要信息")
public class MemberSummaryResp implements Serializable {

    @ApiModelProperty("会员 ID")
    private String memberId;

    @ApiModelProperty("账号编号")
    private String accountNo;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("头像 URL（本轮可为空）")
    private String avatarUrl;

    @ApiModelProperty("脱敏邮箱")
    private String emailMasked;

    @ApiModelProperty("脱敏手机号")
    private String mobileMasked;

    @ApiModelProperty("邮箱是否已验证")
    private Boolean emailVerified;

    @ApiModelProperty("手机是否已验证")
    private Boolean mobileVerified;

    @ApiModelProperty("账号状态")
    private Integer accountStatus;
}
