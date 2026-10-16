package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class RegistrationGapMemberVo {
    private Long playerId;
    private String playerNo;
    private String nickname;
    private String gameId;
    private String blockReason;
}
