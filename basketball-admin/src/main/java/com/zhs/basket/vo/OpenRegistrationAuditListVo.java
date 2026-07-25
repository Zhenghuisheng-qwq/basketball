package com.zhs.basket.vo;

import lombok.Data;

@Data
public class OpenRegistrationAuditListVo {
    private Long id; private String registrationNo; private Long seasonId; private String seasonName; private Long memberId; private Long playerId; private String playerNickname; private String platform; private String displayGameId; private Integer registrationStatus; private String registrationStatusName; private java.util.Date submittedTime; private java.util.Date auditTime;
}
