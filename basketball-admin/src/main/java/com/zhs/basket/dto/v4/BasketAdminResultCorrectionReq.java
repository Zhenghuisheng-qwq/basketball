package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketAdminResultCorrectionReq {
    private Long matchId;
    private Integer homeScore;
    private Integer awayScore;
    private String reason;
    private String remark;
}
