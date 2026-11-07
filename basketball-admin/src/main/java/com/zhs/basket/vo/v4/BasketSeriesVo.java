package com.zhs.basket.vo.v4;

import lombok.Data;

import java.util.Date;

@Data
public class BasketSeriesVo {
    private Long id;
    private String seriesCode;
    private String seriesName;
    private String seriesDesc;
    private String defaultFormatType;
    private Long iconFileId;
    private Long coverFileId;
    private Long bannerFileId;
    private String honorIconUrl;
    private Integer sortOrder;
    private Integer enabled;
    private Date createTime;
    private Date updateTime;
}
