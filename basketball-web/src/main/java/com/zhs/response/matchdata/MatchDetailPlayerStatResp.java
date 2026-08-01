package com.zhs.response.matchdata;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import java.io.Serializable;

@Data
@ApiModel("比赛详情球员数据")
public class MatchDetailPlayerStatResp implements Serializable {

    private String playerId;
    private String rosterId;
    private String playerNo;
    private String playerName;
    private String avatarUrl;
    private String teamNameSnapshot;
    private String eventTeamId;
    private String teamId;
    private String position;
    private String gameId;
    private Integer points;
    private Integer rebounds;
    private Integer assists;
    private Integer steals;
    private Integer blocks;
    private Integer turnovers;
    private Integer fouls;
    private String statusText;
}
