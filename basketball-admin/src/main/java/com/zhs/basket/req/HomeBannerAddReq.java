package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class HomeBannerAddReq {
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
}
