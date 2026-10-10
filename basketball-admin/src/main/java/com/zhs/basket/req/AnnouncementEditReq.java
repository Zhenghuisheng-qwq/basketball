package com.zhs.basket.req;

import lombok.Data;

@Data
public class AnnouncementEditReq {
    private String categoryCode;
    private String title;
    private String titleEn;
    private String summary;
    private String summaryEn;
    private Long coverFileId;
    private String contentText;
    private String contentTextEn;
    private String publicScope;
    private Integer isImportant;
    private Long tournamentId;
    private Long seasonId;
    private String ruleVersion;
    private java.util.Date scheduledPublishTime;
}
