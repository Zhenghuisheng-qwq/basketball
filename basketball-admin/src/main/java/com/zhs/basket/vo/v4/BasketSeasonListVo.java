package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeasonListVo {
    private Long id;
    private Long seriesId;
    private String seriesCode;
    private String seriesName;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String displayTitleEn;
    private String description;
    private String descriptionEn;
    private Long coverFileId;
    private Long bannerFileId;
    private String coverUrl;
    private String bannerUrl;
    private Date seasonStartTime;
    private Date seasonEndTime;
    private String publishStatus;
    private String seasonStatus;
    private Integer divisionCount;
    private Date createTime;
    private Date updateTime;
}
