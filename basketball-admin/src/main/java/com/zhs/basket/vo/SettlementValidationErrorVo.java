package com.zhs.basket.vo;

import lombok.Data;

@Data
public class SettlementValidationErrorVo {
    private Long matchId;
    private String matchNo;
    private String reason;

    public static SettlementValidationErrorVo of(Long matchId, String matchNo, String reason) {
        SettlementValidationErrorVo vo = new SettlementValidationErrorVo();
        vo.setMatchId(matchId);
        vo.setMatchNo(matchNo);
        vo.setReason(reason);
        return vo;
    }
}
