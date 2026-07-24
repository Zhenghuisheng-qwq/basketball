package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("隐私设置响应")
public class PrivacySettingResp implements Serializable {

    @ApiModelProperty("资料公开状态：0 关闭，1 开启")
    private Integer profileVisible;

    @ApiModelProperty("允许球队邀请：0 关闭，1 开启")
    private Integer allowTeamInvite;

    @ApiModelProperty("展示球队历史：0 关闭，1 开启")
    private Integer showTeamHistory;

    @ApiModelProperty("展示比赛数据：0 关闭，1 开启")
    private Integer showMatchData;

    @ApiModelProperty("展示在线状态：0 关闭，1 开启")
    private Integer showOnlineStatus;
}
