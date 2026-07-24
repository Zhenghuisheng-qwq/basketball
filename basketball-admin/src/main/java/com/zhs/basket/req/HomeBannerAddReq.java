package com.zhs.basket.req;

import lombok.Data;

import java.util.Date;

@Data
public class HomeBannerAddReq {
    private String bannerName;
    private String title;
    private String subtitle;
    private Long imageFileId;
    private String positionCode;
    private String targetType;
    private String targetValue;
    private Date startTime;
    private Date endTime;
    private Integer sortNo;
}
