package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ValidationSeverityEnum;
import lombok.Data;

@Data
public class ValidationIssue {
    private ValidationSeverityEnum severity;
    private String code;
    private String message;
    private String unitType;
    private Long unitId;

    public static ValidationIssue error(String code, String message) {
        ValidationIssue issue = new ValidationIssue();
        issue.setSeverity(ValidationSeverityEnum.ERROR);
        issue.setCode(code);
        issue.setMessage(message);
        return issue;
    }

    public static ValidationIssue warning(String code, String message) {
        ValidationIssue issue = new ValidationIssue();
        issue.setSeverity(ValidationSeverityEnum.WARNING);
        issue.setCode(code);
        issue.setMessage(message);
        return issue;
    }
}
