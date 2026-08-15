package com.zhs.basket.support.v4.settlement;

import com.zhs.basket.enums.v4.OfficialResultSource;
import lombok.Data;

@Data
public class OfficialMatchResultCommand {
    private Long matchId;
    private Integer homeScore;
    private Integer awayScore;
    private Long winnerEventTeamId;
    private Long loserEventTeamId;
    private String matchStatus;
    private OfficialResultSource source;
    private Long sourceSubmissionId;
    private String remark;
    private boolean clearOfficialPlayerStats;
    private boolean scoreOnlyTeamStats;
}
