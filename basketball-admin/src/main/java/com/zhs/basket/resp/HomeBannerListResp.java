package com.zhs.basket.resp;

import lombok.Data;

import java.util.Date;

@Data
public class HomeBannerListResp {
    private Long id;
    private String bannerName;
    private String title;
    private String titleEn;
    private String subtitle;
    private String subtitleEn;
    private Long imageFileId;
    private String imageUrl;
    private Long mobileImageFileId;
    private String mobileImageUrl;
    private String altText;
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer bannerStatus;
    private String bannerStatusName;
    private String pendingChangeLabel;
    private Integer hasDraft;
    private Integer draftEnabled;
    private Integer draftRemove;
    private Long draftImageFileId;
    private String draftImageUrl;
    private Long draftMobileImageFileId;
    private String draftMobileImageUrl;
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
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
}
