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
    private int roundNo;
    private int homeScore;
    private int awayScore;
    private int dayOffset;
    private boolean confirmed;
}
