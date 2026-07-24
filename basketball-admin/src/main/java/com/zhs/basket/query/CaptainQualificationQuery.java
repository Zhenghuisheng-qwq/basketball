package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CaptainQualificationQuery extends BaseEntity {
    private String qualificationNo;
    private Long memberId;
    private Long playerId;
    private Integer qualificationStatus;
}
