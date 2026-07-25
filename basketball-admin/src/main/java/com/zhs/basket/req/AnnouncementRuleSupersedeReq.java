package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementRuleSupersedeReq {
    private Long oldAnnouncementId;
    private Long seasonId;
    private Long tournamentId;
    private String ruleVersion;
}
