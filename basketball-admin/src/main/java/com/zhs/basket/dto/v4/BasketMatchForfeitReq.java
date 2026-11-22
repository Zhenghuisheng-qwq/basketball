package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketMatchForfeitReq {
    private Long matchId;
    private Long loserEventTeamId;
    private String reason;
    /** 同时判负该队在本小组尚未开赛的已发布场次。已结束的比赛不动。 */
    private Boolean includeRemaining;
}
