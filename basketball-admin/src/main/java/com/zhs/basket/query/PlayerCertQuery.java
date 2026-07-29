package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlayerCertQuery extends BaseEntity {
    private String applicationNo;
    private String nickname;
    private String email;
    private String gameAccount;
    private Integer applicationStatus;
    /** 审核筛选阶段：PENDING/APPROVED/REJECTED/CANCELLED */
    private String auditStage;
    private Long memberId;
}
