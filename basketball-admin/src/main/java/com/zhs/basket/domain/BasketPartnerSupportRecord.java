package com.zhs.basket.domain;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class BasketPartnerSupportRecord {
    private Long id;
    private Long partnerId;
    private String supportType;
    private BigDecimal supportAmount;
    private Date supportDate;
    private String publicDescription;
    private Integer publishStatus;
    private Integer sortOrder;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
    private Integer isDelete;
}
