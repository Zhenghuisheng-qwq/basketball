package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketV4TeamPlayerOptionVo {
    private Long playerId;
    private String playerName;
    private Long memberId;
    private String memberNickname;
    private Boolean certified;
    private Boolean hasPlatformIdentity;
    private String roleType;
}
