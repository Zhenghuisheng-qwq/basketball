package com.zhs.response.team;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("候选球员项")
public class CandidatePlayerItemResp implements Serializable {

    @ApiModelProperty("会员主键")
    private String memberId;

    @ApiModelProperty("球员主键")
    private String playerId;

    @ApiModelProperty("球员编号")
    private String playerNo;

    @ApiModelProperty("游戏昵称")
    private String gameNickname;

    @ApiModelProperty("昵称")
    private String nickname;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("场上位置")
    private String positionCode;

    @ApiModelProperty("场上位置中文名")
    private String positionName;

    @ApiModelProperty("球员认证状态")
    private Integer certificationStatus;
}
