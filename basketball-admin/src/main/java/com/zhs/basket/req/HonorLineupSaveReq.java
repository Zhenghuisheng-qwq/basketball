package com.zhs.basket.req;

import lombok.Data;

@Data
public class HonorLineupSaveReq {
    private Long playerId;
    private Long teamId;
    private String roleType;
    private String jerseyNo;
    private String positionCode;
    private Integer sortNo;
}
