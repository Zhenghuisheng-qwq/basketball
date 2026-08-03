package com.zhs.response.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class V4MatchSubmissionPlayerStatResp {
    private Long eventPlayerId;
    private String playerName;
    private Integer points;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer foul;
    private BigDecimal minutes;
}
