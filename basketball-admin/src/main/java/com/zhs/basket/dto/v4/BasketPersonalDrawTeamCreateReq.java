package com.zhs.basket.dto.v4;

import lombok.Data;

@Data
public class BasketPersonalDrawTeamCreateReq {
    private String temporaryTeamCode;
    private String temporaryTeamName;
    private Integer expectedMemberCount;
}
