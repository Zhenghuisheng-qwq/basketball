package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPartner {
    private Long id;
    private String partnerName;
    private Long logoFileId;
    private String websiteUrl;
    private String partnerType;
    private Date cooperationStartDate;
    private Date cooperationEndDate;
    private Integer partnerStatus;
    private Integer sortNo;
    private String remark;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
