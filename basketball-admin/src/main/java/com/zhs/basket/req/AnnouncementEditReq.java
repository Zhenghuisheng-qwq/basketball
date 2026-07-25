package com.zhs.basket.req;

import lombok.Data;

@Data
public class AnnouncementEditReq {
    private String categoryCode;
    private String title;
    private String summary;
    private Long coverFileId;
    private String contentText;
    private String publicScope;
    private Integer isImportant;
    private Long tournamentId;
    private Long seasonId;
    private String ruleVersion;
    private java.util.Date scheduledPublishTime;
}
