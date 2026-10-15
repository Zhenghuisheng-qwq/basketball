package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class BasketRegistrationPlayerVo {
    private Long playerId;
    private String playerName;
    private String gameIdSnapshot;
    private String platformCode;
    private String position;
    private String jerseyNo;
    private Integer isCaptain;
    private Integer isStarter;
    /** 1 当前在队，0 报名行仍在但已不是当前队员 */
    private Integer currentMember;
}
