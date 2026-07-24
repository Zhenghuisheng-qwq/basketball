package com.zhs.request.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球员认证草稿保存请求")
public class PlayerCertDraftReq implements Serializable {

    @ApiModelProperty(value = "真实姓名", required = true)
    private String realName;

    @ApiModelProperty(value = "证件类型", required = true)
    private String idType;

    @ApiModelProperty(value = "证件号码（明文）", required = true)
    private String idNo;

    @ApiModelProperty(value = "比赛终端", required = true)
    private String gameTerminal;

    @ApiModelProperty(value = "游戏账号", required = true)
    private String gameAccount;

    @ApiModelProperty(value = "游戏昵称", required = true)
    private String gameNickname;

    @ApiModelProperty(value = "场上位置")
    private String positionCode;

    @ApiModelProperty(value = "联系电话", required = true)
    private String contactMobile;
}
