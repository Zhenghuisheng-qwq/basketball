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
    private String description;
    private Long coverFileId;
    private Long bannerFileId;
    private String registrationMode;
    private String formatType;
    private Date registrationStartTime;
    private Date registrationEndTime;
    private Date matchStartTime;
    private Date matchEndTime;
    private String publishStatus;
    private String seasonStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
