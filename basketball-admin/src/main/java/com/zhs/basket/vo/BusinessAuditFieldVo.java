package com.zhs.basket.vo;

import lombok.Data;

@Data
public class BusinessAuditFieldVo {
    private Long id;
    private String fieldName;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private String dataType;
    private Integer sensitiveFlag;
}
