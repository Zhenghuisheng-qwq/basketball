package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeamAuditQuery extends BaseEntity {
    private String teamNo;
    private String teamName;
    private Integer auditStatus;
    /** 审核筛选阶段：PENDING/APPROVED/REJECTED/CANCELLED */
    private String auditStage;
}
