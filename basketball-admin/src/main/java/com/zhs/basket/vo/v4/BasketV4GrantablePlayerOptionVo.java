package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketV4GrantablePlayerOptionVo {
    private Long playerId;
    private String playerName;
    private Long memberId;
    private String memberNickname;
    private Boolean certified;
    private Boolean hasPlatformIdentity;
    private Boolean hasActiveGrant;
    private String cannotSelectReason;
}
