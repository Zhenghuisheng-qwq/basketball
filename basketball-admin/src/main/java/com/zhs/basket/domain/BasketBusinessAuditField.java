package com.zhs.basket.domain;

import lombok.Data;

import java.util.Date;

@Data
public class BasketBusinessAuditField {
    private Long id;
    private Long auditId;
    private String fieldName;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private String dataType;
    private Integer sensitiveFlag;
    private Date createTime;
    private Date updateTime;
    private Integer isDelete;
}
