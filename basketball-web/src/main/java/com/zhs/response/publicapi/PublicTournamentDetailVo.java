package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开赛事详情")
public class PublicTournamentDetailVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("赛事主键")
    private Long tournamentId;

    @ApiModelProperty("赛事编号")
    private String tournamentNo;

    @ApiModelProperty("赛事名称")
    private String tournamentName;

    @ApiModelProperty("赛事简称")
    private String shortName;

    @ApiModelProperty("英文名称")
    private String englishName;

    @ApiModelProperty("赛事类型")
    private String tournamentType;

    @ApiModelProperty("比赛终端")
    private String gameTerminal;

    @ApiModelProperty("赛制")
    private String formatCode;

    @ApiModelProperty("主办方")
    private String organizerName;

    @ApiModelProperty("赛事介绍")
    private Object description;

    @ApiModelProperty("赛事状态")
    private Integer tournamentStatus;

    @ApiModelProperty("Logo URL")
    private String logoUrl;

    @ApiModelProperty("封面 URL")
    private String coverUrl;

    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty("所属顶层赛季主键")
    private Long competitionSeasonId;

    @ApiModelProperty("所属顶层赛季名称")
    private String competitionSeasonName;

    @ApiModelProperty("所属顶层赛季年份")
    private Integer competitionSeasonYear;
}
