package com.zhs.basket.req;

import lombok.Data;

@Data
public class PlayerHonorCandidateQueryReq {
    private Long seasonId;
    private Long stageId;
    /** SCORING / REBOUND / ASSIST / STEAL / BLOCK 或完整 HonorCode */
    private String awardType;
}
