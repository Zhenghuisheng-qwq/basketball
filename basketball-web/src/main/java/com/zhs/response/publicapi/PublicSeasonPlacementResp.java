package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开赛季名次（冠亚军，需冠军荣誉已发布）")
public class PublicSeasonPlacementResp implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @ApiModelProperty("是否已公开冠亚军（冠军已设置且 CHAMPION 荣誉已发布）")
    private Boolean published;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("冠军球队主键")
    private Long championTeamId;

    @ApiModelProperty("冠军球队名称")
    private String championTeamName;

    @ApiModelProperty("冠军球队 Logo")
    private String championTeamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("亚军球队主键")
    private Long runnerUpTeamId;

    @ApiModelProperty("亚军球队名称")
    private String runnerUpTeamName;

    @ApiModelProperty("亚军球队 Logo")
    private String runnerUpTeamLogoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("冠军荣誉主键")
    private Long championHonorId;
}
