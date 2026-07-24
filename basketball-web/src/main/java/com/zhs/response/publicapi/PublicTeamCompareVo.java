package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开球队对比")
public class PublicTeamCompareVo implements Serializable {

    @ApiModelProperty("左侧球队")
    private PublicTeamDetailVo leftTeam;

    @ApiModelProperty("右侧球队")
    private PublicTeamDetailVo rightTeam;

    @ApiModelProperty("左侧赛季统计")
    private PublicTeamSeasonStatVo leftStat;

    @ApiModelProperty("右侧赛季统计")
    private PublicTeamSeasonStatVo rightStat;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;
}
