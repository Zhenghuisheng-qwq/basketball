package com.zhs.basket.vo;

import lombok.Data;

@Data
public class TeamAuditMemberVo {
    private Long memberId;
    private Long playerId;
    private String nickname;
    private String playerNo;
    private String roleType;
    private String roleTypeName;
}
