package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketMatchDataDisputeMaterial {
    private Long id;
    private Long disputeId;
    private Long matchId;
    private Long fileId;
    private String materialType;
    private String materialName;
    private Integer sortNo;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
