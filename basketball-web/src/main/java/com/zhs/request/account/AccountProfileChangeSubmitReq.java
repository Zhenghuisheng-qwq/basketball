package com.zhs.request.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("账号公开资料变更提交")
public class AccountProfileChangeSubmitReq implements Serializable {

    @ApiModelProperty("申请昵称")
    private String nickname;

    @ApiModelProperty("申请头像文件ID；不传表示沿用当前正式头像")
    private String avatarFileId;
}
