package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketHonorRecord {
    private Long id;
    private Long seriesId;
    private Long seasonId;
    private Long honorCategoryId;
    private Long teamId;
    private Long playerId;
    private Long matchId;
    private String honorTitle;
    private String honorSubtitle;
    private String honorValue;
    private Date awardDate;
    private Long coverFileId;
    private String sourceType;
    private Long sourceId;
    private Long generationBatchId;
    private Integer displayStatus;
    private Integer displayOrder;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
