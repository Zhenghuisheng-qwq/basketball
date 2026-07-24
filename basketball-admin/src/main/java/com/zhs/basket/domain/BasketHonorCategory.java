package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketHonorCategory {
    private Long id;
    private String honorCode;
    private String honorName;
    private String honorScope;
    private String recipientType;
    private String description;
    private Integer displayOrder;
    private Integer honorStatus;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
