package com.zhs.basket.dto;

import lombok.Data;

@Data
public class SeasonChampionDto {
    private Long seasonId;
    private Long tournamentId;
    private String seasonName;
    private Integer seasonYear;
    private Integer seasonStatus;
    private Long championTeamId;
    private String championTeamName;
    private Long runnerUpTeamId;
    private String runnerUpTeamName;
}
