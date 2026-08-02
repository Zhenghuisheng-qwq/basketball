package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketPartner {
    private Long id;
    private String partnerCode;
    private String partnerName;
    private Long logoFileId;
    private String websiteUrl;
    private String publicDescription;
    private String partnerType;
    private Date cooperationStartDate;
    private Date cooperationEndDate;
    private Integer partnerStatus;
    private Integer sortNo;
    private String remark;
    private String createBy;
    private String updateBy;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
