package com.zhs.basket.vo;

import com.zhs.basket.domain.BasketMatchExceptionApplication;
import com.zhs.basket.domain.BasketMatchExceptionAudit;
import lombok.Data;

import java.util.List;

@Data
public class MatchExceptionDetailVo {
    private BasketMatchExceptionApplication application;
    private List<MatchDataSubmissionMaterialVo> materials;
    private List<BasketMatchExceptionAudit> audits;
    private String matchNo;
    private Long homeTeamId;
    private Long awayTeamId;
}
