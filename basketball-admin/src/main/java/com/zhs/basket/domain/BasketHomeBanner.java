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
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer bannerStatus;
    private Integer sortNo;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
