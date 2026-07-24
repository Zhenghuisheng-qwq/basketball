package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketAnnouncement {
    private Long id;
    private String announcementNo;
    private String categoryCode;
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
    private Date offlineTime;
    private Long viewCount;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
