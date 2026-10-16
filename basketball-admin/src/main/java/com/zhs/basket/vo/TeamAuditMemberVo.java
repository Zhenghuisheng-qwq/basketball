package com.zhs.basket.vo;

import lombok.Data;

@Data
public class TeamAuditMemberVo {
    private Long memberId;
    private Long playerId;
    private String nickname;
    private String playerNo;
    /** 当前赛区平台下已认证的游戏 ID，不是球员主键。 */
    private String gameId;
    private String roleType;
    private String roleTypeName;
}
