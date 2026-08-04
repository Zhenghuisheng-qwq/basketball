package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketScheduleBatch
 */
@Data
public class BasketScheduleBatch {
    private Long id;
    private Long seasonId;
    private Long divisionId;
    private Long stageId;
    private Long groupingBatchId;
    private String batchNo;
    private String formatType;
    private String ruleCode;
    private String generationStrategy;
    private Integer teamCount;
    private Integer matchCount;
    private Integer expectedMatchCount;
    private Integer actualMatchCount;
    private Integer executionVersion;
    private String generateType;
    private String generateStatus;
    private String publishStatus;
    private Date generateTime;
    private Date publishTime;
    private Long generatedBy;
    private Long publishedBy;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
