package com.zhs.basket.query;

import com.zhs.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TournamentQuery extends BaseEntity {
    private String tournamentNo;
    private String tournamentName;
    private Integer tournamentStatus;
    private Long competitionSeasonId;
}
