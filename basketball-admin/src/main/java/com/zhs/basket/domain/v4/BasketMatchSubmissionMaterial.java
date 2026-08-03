package com.zhs.basket.domain.v4;

import lombok.Data;
import java.util.Date;

/**
 * CNPA 赛事域 V4 实体：BasketMatchSubmissionMaterial
 */
@Data
public class BasketMatchSubmissionMaterial {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Long divisionId;
    private String materialType;
    private Long fileId;
    private String fileName;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
