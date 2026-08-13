package com.zhs.basket.support.v4.version;

import lombok.Data;

/**
 * 结构生成时的版本上下文。rosterVersion 允许为空（Legacy）。
 */
@Data
public class StructureVersionContext {
    private Long divisionId;
    private Long ruleVersionId;
    private Integer ruleVersionNo;
    private Long rosterVersionId;
    private Integer rosterVersionNo;
    private Long pairingExecutionId;
}
