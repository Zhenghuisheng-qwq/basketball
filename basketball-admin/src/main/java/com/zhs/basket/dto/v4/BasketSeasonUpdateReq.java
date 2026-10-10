package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonUpdateReq {
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String displayTitleEn;
    private String description;
    private String descriptionEn;
    private Long coverFileId;
    private Long bannerFileId;
    private Date seasonStartTime;
    private Date seasonEndTime;
}
