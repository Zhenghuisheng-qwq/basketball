package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonUpdateReq {
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private String registrationMode;
    private String formatType;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
}
