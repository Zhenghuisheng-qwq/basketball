package com.zhs.basket.req;

import lombok.Data;

@Data
public class MatchForfeitReq {
    /** 判胜球队主键 */
    private Long walkoverTeamId;
    private String reason;
}
