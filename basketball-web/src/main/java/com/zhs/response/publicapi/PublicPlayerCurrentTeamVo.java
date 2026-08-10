package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开球员当前参赛球队")
public class PublicPlayerCurrentTeamVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事球队主键")
    private Long teamId;

    private String teamName;
    private String shortName;
    private String logoUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long divisionId;
    private String divisionName;
    private String platformCode;
    private String gameMode;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String seasonName;
    private String seriesName;

    @ApiModelProperty("成员角色 CAPTAIN/MEMBER")
    private String myRole;
}
