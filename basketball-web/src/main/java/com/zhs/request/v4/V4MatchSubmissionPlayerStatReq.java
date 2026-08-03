package com.zhs.request.v4;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class V4MatchSubmissionPlayerStatReq {
    private Long eventPlayerId;
    private Integer points;
    private Integer rebound;
    private Integer assist;
    private Integer steal;
    private Integer block;
    private Integer turnover;
    private Integer foul;
    private BigDecimal minutes;
}
