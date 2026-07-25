package com.zhs.basket.vo;

import lombok.Data;

@Data
public class EligibleCaptainVo {
    private Long playerId;
    /** 用户ID（历史字段 memberId） */
    private Long memberId;
    private Long userId;
    private String playerNo;
    private String nickname;
    private String gameNickname;
    private Integer certificationStatus;
}
