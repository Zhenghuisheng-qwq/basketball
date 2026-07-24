package com.zhs.basket.resp;

import lombok.Data;

@Data
public class HonorLineupResp {
    private Long id;
    private Long honorRecordId;
    private Long teamId;
    private Long playerId;
    private Long memberId;
    private String playerName;
    private String roleType;
    private String jerseyNo;
    private String positionCode;
    private Integer sortNo;
    private String avatarUrl;
}
