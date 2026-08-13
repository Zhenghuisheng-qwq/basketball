package com.zhs.basket.support.v4.schedule;

import com.zhs.basket.enums.v4.ValidationSeverityEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ValidationResult {
    private final List<ValidationIssue> issues = new ArrayList<>();

    public void add(ValidationIssue issue) {
        if (issue != null) {
            issues.add(issue);
        }
    }

    public void addAll(List<ValidationIssue> more) {
        if (more != null) {
            issues.addAll(more);
        }
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(i -> i.getSeverity() == ValidationSeverityEnum.ERROR);
    }

    public boolean canPublish() {
        return !hasErrors();
    }

    public List<ValidationIssue> errors() {
        return issues.stream().filter(i -> i.getSeverity() == ValidationSeverityEnum.ERROR).toList();
    }

    public List<ValidationIssue> warnings() {
        return issues.stream().filter(i -> i.getSeverity() == ValidationSeverityEnum.WARNING).toList();
    }
}
