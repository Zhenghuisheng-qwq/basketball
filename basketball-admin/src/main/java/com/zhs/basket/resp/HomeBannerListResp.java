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
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer bannerStatus;
    private String bannerStatusName;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
}
