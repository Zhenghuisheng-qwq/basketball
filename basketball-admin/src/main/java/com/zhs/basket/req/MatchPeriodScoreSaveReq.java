package com.zhs.basket.req;

import lombok.Data;
import java.util.List;

@Data
public class MatchPeriodScoreSaveReq {
    private List<PeriodScoreItem> periods;

    @Data
    public static class PeriodScoreItem {
        private Integer periodNo;
        private String periodType;
        private Integer homeScore;
        private Integer awayScore;
    }
}
