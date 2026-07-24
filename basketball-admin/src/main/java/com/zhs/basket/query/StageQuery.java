package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class StageQuery extends BaseEntity {
    private Long tournamentId;
    private Long seasonId;
    private String stageName;
    private String stageType;
    private Integer stageStatus;
}
