package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class HomeBannerEditReq {
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
    private Integer sortNo;
    /** 1 发布后在首页显示，0 发布后停用 */
    private Integer draftEnabled;
}
