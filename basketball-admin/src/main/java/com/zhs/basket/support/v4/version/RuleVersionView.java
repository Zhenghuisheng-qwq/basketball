package com.zhs.basket.support.v4.version;

import lombok.Data;

/**
 * Division Rule Version 统一读取对象。
 * 底层实现仍是 FormatSnapshot，不另建平行模型。
 */
@Data
public class RuleVersionView {
    private Long ruleVersionId;
    private Integer ruleVersionNo;
    private String ruleVersionStatus;
    private Long divisionId;
    private Integer isCurrent;
    private boolean locked;
}
