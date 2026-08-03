package com.zhs.basket.dto.v4;

import lombok.Data;
import java.util.Date;

@Data
public class BasketCaptainGrantCreateReq {
    private Long divisionId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private Date validFrom;
    private Date validTo;
    private String remark;
}
