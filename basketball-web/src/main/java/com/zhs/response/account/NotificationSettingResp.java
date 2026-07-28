package com.zhs.response.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("通知设置响应")
public class NotificationSettingResp implements Serializable {

    @ApiModelProperty("系统通知：0 关闭，1 开启")
    private Integer systemNotice;

    @ApiModelProperty("认证通知：0 关闭，1 开启")
    private Integer certificationNotice;

    @ApiModelProperty("赛事通知：0 关闭，1 开启")
    private Integer tournamentNotice;

    @ApiModelProperty("球队通知：0 关闭，1 开启")
    private Integer teamNotice;

    @ApiModelProperty("公告通知：0 关闭，1 开启")
    private Integer announcementNotice;
}
