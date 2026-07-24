package com.zhs.basket.localdemo.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DemoMatchDto {
    private int matchIndex;
    private String matchNo;
    private String homeTeamKey;
    private String awayTeamKey;
    /** 小组标识：A / B / C / D */
    private String groupKey;
    private int roundNo;
    private int homeScore;
    private int awayScore;
    private int dayOffset;
    private boolean confirmed;
}
