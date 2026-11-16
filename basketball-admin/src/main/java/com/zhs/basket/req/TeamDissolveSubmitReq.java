package com.zhs.basket.req;

import lombok.Data;

@Data
public class TeamDissolveSubmitReq {
    private Long teamId;
    private String reason;
}
