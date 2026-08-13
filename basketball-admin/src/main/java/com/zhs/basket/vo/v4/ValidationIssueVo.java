package com.zhs.basket.vo.v4;

import lombok.Data;

@Data
public class ValidationIssueVo {
    private String severity;
    private String code;
    private String message;
    private String unitType;
    private Long unitId;
}
