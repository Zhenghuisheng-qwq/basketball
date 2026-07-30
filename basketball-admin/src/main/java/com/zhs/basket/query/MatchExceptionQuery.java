package com.zhs.basket.query;

import lombok.Data;

@Data
public class MatchExceptionQuery {
    private Long seasonId;
    private Long matchId;
    private String applicationStatus;
    private String exceptionType;
}
