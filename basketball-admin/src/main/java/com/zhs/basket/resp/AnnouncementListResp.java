package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class AnnouncementListResp {
    private Long id;
    private String announcementNo;
    private String categoryCode;
    private String categoryName;
    private String title;
    private String summary;
    private Long coverFileId;
    private String coverUrl;
    private Long authorId;
    private String authorName;
    private Integer announcementStatus;
    private String announcementStatusName;
    private String publicScope;
    private Integer isTop;
    private Integer isImportant;
    private Date publishTime;
    private Date offlineTime;
    private Long viewCount;
    private Date createTime;
    private Date updateTime;
}
