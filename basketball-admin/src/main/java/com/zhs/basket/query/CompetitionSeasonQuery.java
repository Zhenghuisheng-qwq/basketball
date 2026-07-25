package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CompetitionSeasonQuery extends BaseEntity {
    private String seasonName;
    private Integer seasonYear;
    private Integer seasonStatus;
}
