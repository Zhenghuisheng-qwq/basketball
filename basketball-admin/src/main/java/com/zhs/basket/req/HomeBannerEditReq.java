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
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer sortNo;
}
