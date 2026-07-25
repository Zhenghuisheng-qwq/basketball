package com.zhs.basket.req;

import lombok.Data;

@Data
public class AuditFieldInput {
    private String fieldName;
    private String fieldLabel;
    private String beforeValue;
    private String afterValue;
    private String dataType;
    private Integer sensitiveFlag;
}
