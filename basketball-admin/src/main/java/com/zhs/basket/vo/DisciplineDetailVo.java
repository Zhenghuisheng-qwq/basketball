package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketDisciplineRecord;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class DisciplineDetailVo extends BasketDisciplineRecord {
    private String seasonName;
    private String seriesName;
    private String teamName;
    private String eventTeamName;
    private String playerNo;
    private String playerName;
}
