package com.zhs.basket.vo;

import lombok.Data;

@Data
public class MatchDataSubmissionSubmitterVo {
    private Long userId;
    private String userName;
    private String nickName;
    private String displayName;
    private String avatarUrl;
    private String displayEmail;
    private Long playerId;
    private String playerCode;
    private String playerName;
    private String gameId;
    private String role;
    private String roleText;
    private String teamName;
    private Boolean missing;
}
