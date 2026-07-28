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

    @ApiModelProperty("历史记录主键")
    private String historyId;

    @ApiModelProperty("变更类型")
    private String changeType;

    @ApiModelProperty("变更类型名称")
    private String changeTypeName;

    @ApiModelProperty("平台代码")
    private String platformCode;

    @ApiModelProperty("平台名称")
    private String platformName;

    @ApiModelProperty("游戏平台（兼容字段）")
    private String platform;

    @ApiModelProperty("变更前游戏 ID")
    private String oldGameId;

    @ApiModelProperty("变更后游戏 ID")
    private String newGameId;

    @ApiModelProperty("变更前游戏 ID（兼容字段）")
    private String oldDisplayGameId;

    @ApiModelProperty("变更后游戏 ID（兼容字段）")
    private String newDisplayGameId;

    @ApiModelProperty("生效时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date effectiveTime;

    @ApiModelProperty("申请编号（兼容字段，可能为空）")
    private String requestNo;
}
