package com.zhs.basket.req;

import lombok.Data;

@Data
public class TeamCaptainTransferReq {
    private Long teamId;
    private Long teamMemberId;
}
