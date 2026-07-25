package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开赛季概览")
public class PublicSeasonOverviewVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛季主键")
    private Long seasonId;

    @ApiModelProperty("赛季名称")
    private String seasonName;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事主键")
    private Long tournamentId;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛季状态")
    private Integer seasonStatus;

    @ApiModelProperty("球队数")
    private Integer teamCount;

    @ApiModelProperty("已结束并确认的比赛数")
    private Integer finishedMatchCount;

    @ApiModelProperty("可公开赛程数")
    private Integer publicMatchCount;

    @ApiModelProperty("积分榜记录数")
    private Integer standingCount;

    @ApiModelProperty("描述")
    private Object description;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("所属顶层赛季主键")
    private Long competitionSeasonId;

    @ApiModelProperty("所属顶层赛季名称")
    private String competitionSeasonName;

    @ApiModelProperty("所属顶层赛季年份")
    private Integer competitionSeasonYear;
}
