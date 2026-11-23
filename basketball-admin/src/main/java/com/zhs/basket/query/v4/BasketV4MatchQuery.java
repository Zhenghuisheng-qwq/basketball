package com.zhs.basket.query.v4;

import lombok.Data;

@Data
public class BasketV4MatchQuery {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long scheduleBatchId;
    private Integer roundNo;
    private String matchStatus;
    private String publishStatus;
    private Long eventTeamId;
    private String teamName;
    private Long stageId;
    private Long seriesId;
    /** 比赛日期起，含当天。系列赛单场没有独立时间时按系列赛开赛时间。 */
    private String matchDateBegin;
    /** 比赛日期止，含当天。 */
    private String matchDateEnd;
}
