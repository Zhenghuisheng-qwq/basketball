package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4MatchSubmissionEligiblePlayerResp {
    private Long eventPlayerId;
    private String playerName;
    private String gameId;
    private String jerseyNo;
    private String position;
    private Integer isCaptain;
}
