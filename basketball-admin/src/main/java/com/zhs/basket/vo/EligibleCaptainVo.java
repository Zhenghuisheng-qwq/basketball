package com.zhs.basket.vo;

import lombok.Data;

@Data
public class EligibleCaptainVo {
    private Long playerId;
    private Long memberId;
    private String playerNo;
    private String realName;
    private Integer certificationStatus;
}
