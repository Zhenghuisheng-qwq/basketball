package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class TeamRosterPromoteReq {
    private Long formalRegistrationId;
    private Long candidateRegistrationId;
    private String reason;
}
