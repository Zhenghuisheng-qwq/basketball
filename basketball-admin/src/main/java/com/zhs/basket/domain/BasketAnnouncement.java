package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketAnnouncement {
    private Long id;
    private String announcementNo;
    private String categoryCode;
    private Long tournamentId;
    private Long seasonId;
    private String title;
    private String summary;
    private Long coverFileId;
    private String contentText;
    private Long authorId;
    private String authorName;
    private Integer announcementStatus;
    private String publicScope;
    private Integer isTop;
    private Integer isImportant;
    private Date publishTime;
    private Date scheduledPublishTime;
    private Date offlineTime;
    private Long supersededBy;
    private String ruleVersion;
    private Long viewCount;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
