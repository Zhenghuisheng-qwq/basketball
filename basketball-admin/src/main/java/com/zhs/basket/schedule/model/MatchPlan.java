package com.zhs.basket.schedule.model;

import lombok.Data;
import java.util.Date;

@Data
public class MatchPlan {
    private Long homeEventTeamId;
    private Long awayEventTeamId;
    private Long homeTeamId;
    private Long awayTeamId;
    private String homeTeamNameSnapshot;
    private String awayTeamNameSnapshot;
    private String homeTeamLogoSnapshot;
    private String awayTeamLogoSnapshot;
    private Date matchTime;
    private Integer roundNo;
    private Integer sortNo;
}
