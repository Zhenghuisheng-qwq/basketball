package com.zhs.basket.req;

import lombok.Data;

@Data
public class SeasonResultConfirmReq {
    private Long championTeamId;
    private Long runnerUpTeamId;
    private Long thirdPlaceTeamId;
    private String confirmRemark;
}
