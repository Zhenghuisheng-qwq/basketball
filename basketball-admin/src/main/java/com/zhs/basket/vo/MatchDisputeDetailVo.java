package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketMatchDataDispute;
import lombok.Data;

import java.util.List;

@Data
public class MatchDisputeDetailVo {
    private BasketMatchDataDispute dispute;
    private List<MatchDataSubmissionMaterialVo> materials;
    private String matchNo;
    private Long homeTeamId;
    private Long awayTeamId;
    private Integer homeScore;
    private Integer awayScore;
}
