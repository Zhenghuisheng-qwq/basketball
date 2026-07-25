package com.zhs.response.player;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("球员参赛资格状态")
public class PlayerEligibilityStatusResp implements Serializable {

    @ApiModelProperty("永久球员编号")
    private String playerNo;

    @ApiModelProperty("球员状态")
    private Integer playerStatus;

    @ApiModelProperty("球员状态说明")
    private String playerStatusText;

    @ApiModelProperty("是否可正常参赛")
    private Boolean eligible;

    @ApiModelProperty("提示说明")
    private String notice;
}
