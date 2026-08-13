package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class PersonalFormationMoveReq {
    private Long personalRegistrationId;
    private Long fromDrawTeamId;
    private Long toDrawTeamId;
}
