package com.zhs.basket.dto.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonCreateReq {
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private Date seasonStartTime;
    private Date seasonEndTime;
}
