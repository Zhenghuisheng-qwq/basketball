package com.zhs.response.publicapi;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
@ApiModel("名人堂概览")
public class PublicHallOverviewResp implements Serializable {

    @ApiModelProperty("赛事荣誉数量")
    private Long tournamentHonorCount;

    @ApiModelProperty("球员荣誉数量")
    private Long playerHonorCount;

    @ApiModelProperty("球队荣誉数量")
    private Long teamHonorCount;

    @ApiModelProperty("近期赛事荣誉")
    private List<PublicHonorItemResp> latestTournamentHonors;

    @ApiModelProperty("近期球员荣誉")
    private List<PublicHonorItemResp> latestPlayerHonors;

    @ApiModelProperty("近期球队荣誉")
    private List<PublicHonorItemResp> latestTeamHonors;
}
