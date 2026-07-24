package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RegistrationAuditQuery extends BaseEntity {
    private String registrationNo;
    private Long seasonId;
    private Long teamId;
    private Integer registrationStatus;
}
