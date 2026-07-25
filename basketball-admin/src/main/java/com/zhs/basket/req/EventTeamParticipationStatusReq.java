package com.zhs.basket.req;

import lombok.Data;

@Data
public class EventTeamParticipationStatusReq {
    private Integer participationStatus;
    private String reason;
}
