package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@ApiModel("公开球队赛季历史")
public class PublicTeamSeasonHistoryVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String seasonName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tournamentId;
    private String tournamentName;
    private String displayName;
    private Integer finalRank;
    private Integer seasonStatus;
    private Integer games;
    private Integer wins;
    private Integer losses;
    private BigDecimal winRate;
}
