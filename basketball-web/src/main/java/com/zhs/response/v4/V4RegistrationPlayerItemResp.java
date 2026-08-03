package com.zhs.response.v4;

import lombok.Data;

@Data
public class V4RegistrationPlayerItemResp {
    private Long playerId;
    private String playerName;
    private Integer isCaptain;
    private Integer sortOrder;
}
