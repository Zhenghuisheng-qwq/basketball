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
}
