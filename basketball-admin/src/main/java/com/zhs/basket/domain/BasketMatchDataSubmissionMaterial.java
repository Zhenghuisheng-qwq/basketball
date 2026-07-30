package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataSubmissionMaterial {
    private Long id;
    private Long submissionId;
    private Long matchId;
    private Long fileId;
    private String materialType;
    private String materialName;
    private Integer sortNo;
    private Integer materialStatus;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
