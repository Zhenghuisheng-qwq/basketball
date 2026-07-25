package com.zhs.response.player;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.util.Date;

@Data
@ApiModel("平台游戏 ID 变更历史项")
public class PlatformChangeHistoryItemResp implements Serializable {

    @ApiModelProperty("申请编号")
    private String requestNo;

    @ApiModelProperty("变更类型")
    private String changeType;

    @ApiModelProperty("游戏平台")
    private String platform;

    @ApiModelProperty("变更前游戏 ID")
    private String oldDisplayGameId;

    @ApiModelProperty("变更后游戏 ID")
    private String newDisplayGameId;

    @ApiModelProperty("申请状态")
    private Integer requestStatus;

    @ApiModelProperty("申请状态说明")
    private String requestStatusText;

    @ApiModelProperty("提交时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date submitTime;

    @ApiModelProperty("审核完成时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date reviewedTime;
}
