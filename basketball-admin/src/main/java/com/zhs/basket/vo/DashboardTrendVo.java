package com.zhs.basket.vo;

import lombok.Data;

import java.util.List;

@Data
public class DashboardTrendVo {
    private List<String> dateList;
    private List<DashboardTrendSeriesVo> series;
    private DashboardTrendSummaryVo summary;
}
