package com.zhs.response.publicapi;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("公开赛季汇总")
public class PublicSeasonSummaryVo implements Serializable {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long seasonId;
    private String seasonName;
    @JsonSerialize(using = ToStringSerializer.class)
    private Long tournamentId;
    private String tournamentName;
    private Integer seasonStatus;
    private Integer teamCount;
    private Integer matchCount;
    private Integer finishedMatchCount;
    private Integer standingCount;
    private Integer teamStatCount;
    private Integer playerStatCount;
}
