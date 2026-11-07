package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketSeries
 */
@Data
public class BasketSeries {
    private Long id;
    private String seriesCode;
    private String seriesName;
    private String seriesDesc;
    private String defaultFormatType;
    private Long iconFileId;
    private Long coverFileId;
    private Long bannerFileId;
    /** 球员荣誉徽章图，按赛事系列一张。 */
    private String honorIconUrl;
    private Integer sortOrder;
    private Integer enabled;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
