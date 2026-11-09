package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketHomeBanner {
    private Long id;
    private String bannerName;
    private String title;
    private String titleEn;
    private String subtitle;
    private String subtitleEn;
    private Long imageFileId;
    private Long mobileImageFileId;
    private String altText;
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer bannerStatus;
    private Integer sortNo;
    private Integer hasDraft;
    private Integer draftEnabled;
    private Integer draftRemove;
    private Long draftImageFileId;
    private Long draftMobileImageFileId;
    private String draftAltText;
    private String draftTitle;
    private String draftTitleEn;
    private String draftSubtitle;
    private String draftSubtitleEn;
    private String draftTargetType;
    private String draftTargetValue;
    private Integer draftSortNo;
    private Date draftStartTime;
    private Date draftEndTime;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
