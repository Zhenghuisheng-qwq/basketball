package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketSeason
 */
@Data
public class BasketSeason {
    private Long id;
    private Long seriesId;
    private Integer seasonYear;
    private String seasonName;
    private String displayTitle;
    private String displayTitleEn;
    private String description;
    private String descriptionEn;
    private Long coverFileId;
    private Long bannerFileId;
    /** 赛季展示周期开始（DB: match_start_time） */
    private Date seasonStartTime;
    /** 赛季展示周期结束（DB: match_end_time） */
    private Date seasonEndTime;
    private String publishStatus;
    private String seasonStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
